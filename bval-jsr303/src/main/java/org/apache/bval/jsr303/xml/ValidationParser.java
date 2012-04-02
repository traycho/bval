/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.bval.jsr303.xml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.validation.ConstraintValidatorFactory;
import javax.validation.MessageInterpolator;
import javax.validation.TraversableResolver;
import javax.validation.ValidationException;
import javax.validation.spi.ValidationProvider;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.bval.jsr303.ConfigurationImpl;
import org.apache.bval.jsr303.util.IOUtils;
import org.apache.bval.jsr303.util.Privileged;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

/**
 * Description: uses jaxb to parse validation.xml<br/>
 */
@SuppressWarnings("restriction")
public class ValidationParser {
    private static final String DEFAULT_VALIDATION_XML_FILE = "META-INF/validation.xml";
    private static final String VALIDATION_CONFIGURATION_XSD =
            "META-INF/validation-configuration-1.0.xsd";
    private static final Logger log = Logger.getLogger(ValidationParser.class.getName());
    private static final Privileged PRIVILEGED = new Privileged();

    private final String validationXmlFile;

    /**
     * Create a new ValidationParser instance.
     *
     * @param file
     */
    public ValidationParser(String file) {
        this.validationXmlFile = ObjectUtils.defaultIfNull(file, DEFAULT_VALIDATION_XML_FILE);
    }

    /**
     * Process the validation configuration into <code>targetConfig</code>.
     *
     * @param targetConfig
     */
    public void processValidationConfig(ConfigurationImpl targetConfig) {
        ValidationConfigType xmlConfig = parseXmlConfig();
        if (xmlConfig != null) {
            applyConfig(xmlConfig, targetConfig);
        }
    }

    private ValidationConfigType parseXmlConfig() {
        InputStream inputStream = null;
        try {
            inputStream = getInputStream(validationXmlFile);
            if (inputStream == null) {
            	log.log(Level.FINEST, String.format("No %s found. Using annotation based configuration only.", validationXmlFile));
                return null;
            }
            log.log(Level.FINEST, String.format("%s found.", validationXmlFile));

            return PRIVILEGED.unmarshallXml(getSchema(), inputStream, ValidationConfigType.class);
        } catch (Exception e) {
            throw new ValidationException("Unable to parse " + validationXmlFile, e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    static InputStream getInputStream(String path) throws IOException {
        final ClassLoader loader = PRIVILEGED.getClassLoader(ValidationParser.class);
        InputStream inputStream = loader.getResourceAsStream(path);

        if (inputStream != null) {
            // spec says: If more than one META-INF/validation.xml file
            // is found in the classpath, a ValidationException is raised.
            Enumeration<URL> urls = loader.getResources(path);
            if (urls.hasMoreElements()) {
                String url = urls.nextElement().toString();
                while (urls.hasMoreElements()) {
                    if (!url.equals(urls.nextElement().toString())) { // complain when first duplicate found
                        throw new ValidationException("More than one " + path + " is found in the classpath");
                    }
                }
            }
        }

        return inputStream;
    }

    private static Schema getSchema() {
        return getSchema(VALIDATION_CONFIGURATION_XSD);
    }

    /**
     * Get a Schema object from the specified resource name.
     *
     * @param xsd
     * @return {@link Schema}
     */
    static Schema getSchema(String xsd) {
        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL schemaUrl = PRIVILEGED.getClassLoader(ValidationParser.class).getResource(xsd);
        try {
            return sf.newSchema(schemaUrl);
        } catch (SAXException e) {
            log.log(Level.WARNING, String.format("Unable to parse schema: %s", xsd), e);
            return null;
        }
    }

    private void applyConfig(ValidationConfigType xmlConfig, ConfigurationImpl targetConfig) {
        applyProviderClass(xmlConfig, targetConfig);
        applyMessageInterpolator(xmlConfig, targetConfig);
        applyTraversableResolver(xmlConfig, targetConfig);
        applyConstraintFactory(xmlConfig, targetConfig);
        applyMappingStreams(xmlConfig, targetConfig);
        applyProperties(xmlConfig, targetConfig);
    }

    private void applyProperties(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        for (PropertyType property : xmlConfig.getProperty()) {
            if (log.isLoggable(Level.FINEST)) {
                log.log(Level.FINEST, String.format(
                    "Found property '%s' with value '%s' in %s",
                    property.getName(), property.getValue(), validationXmlFile));
            }
            target.addProperty(property.getName(), property.getValue());
        }
    }

    private void applyProviderClass(ValidationConfigType xmlConfig, ConfigurationImpl target) {
        String providerClassName = xmlConfig.getDefaultProvider();
        if (providerClassName != null) {
            @SuppressWarnings("unchecked")
            Class<? extends ValidationProvider<?>> clazz =
                (Class<? extends ValidationProvider<?>>) loadClass(providerClassName);
            target.setProviderClass(clazz);
            log.log(Level.INFO, String.format("Using %s as validation provider.", providerClassName));
        }
    }

    private void applyMessageInterpolator(ValidationConfigType xmlConfig,
                                          ConfigurationImpl target) {
        String messageInterpolatorClass = xmlConfig.getMessageInterpolator();
        if (target.getMessageInterpolator() == null) {
            if (messageInterpolatorClass != null) {
                @SuppressWarnings("unchecked")
                Class<? extends MessageInterpolator> clazz =
                    (Class<? extends MessageInterpolator>) loadClass(messageInterpolatorClass);
                target.messageInterpolator(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as message interpolator.", messageInterpolatorClass));
            }
        }
    }

    private void applyTraversableResolver(ValidationConfigType xmlConfig,
                                          ConfigurationImpl target) {
        String traversableResolverClass = xmlConfig.getTraversableResolver();
        if (target.getTraversableResolver() == null) {
            if (traversableResolverClass != null) {
                @SuppressWarnings("unchecked")
                Class<? extends TraversableResolver> clazz =
                    (Class<? extends TraversableResolver>) loadClass(traversableResolverClass);
                target.traversableResolver(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as traversable resolver.", traversableResolverClass));
            }
        }
    }

    private void applyConstraintFactory(ValidationConfigType xmlConfig,
                                        ConfigurationImpl target) {
        String constraintFactoryClass = xmlConfig.getConstraintValidatorFactory();
        if (target.getConstraintValidatorFactory() == null) {
            if (constraintFactoryClass != null) {
                @SuppressWarnings("unchecked")
                Class<? extends ConstraintValidatorFactory> clazz =
                    (Class<? extends ConstraintValidatorFactory>) loadClass(constraintFactoryClass);
                target.constraintValidatorFactory(newInstance(clazz));
                log.log(Level.INFO, String.format("Using %s as constraint factory.", constraintFactoryClass));
            }
        }
    }

    private void applyMappingStreams(ValidationConfigType xmlConfig,
                                     ConfigurationImpl target) {
        for (JAXBElement<String> mappingFileNameElement : xmlConfig.getConstraintMapping()) {
            // Classloader needs a path without a starting /
            String mappingFileName = StringUtils.removeStart(mappingFileNameElement.getValue(), "/");
            log.log(Level.FINEST, String.format("Trying to open input stream for %s", mappingFileName));
            InputStream in = null;
            try {
                in = getInputStream(mappingFileName);
                if (in == null) {
                    throw new ValidationException("Unable to open input stream for mapping file " + mappingFileName);
                }
            } catch (IOException e) {
                throw new ValidationException("Unable to open input stream for mapping file " + mappingFileName, e);
            }
            target.addMapping(in);
        }
    }

    private static <T> T newInstance(final Class<T> cls) {
        try {
            return PRIVILEGED.run(new PrivilegedExceptionAction<T>() {

                public T run() throws Exception {
                    return cls.newInstance();
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> loadClass(final String className) {
        try {
            return PRIVILEGED.getClass(PRIVILEGED.getClassLoader(ValidationParser.class), className);
        } catch (ClassNotFoundException e) {
            throw new ValidationException(String.format("Unable to load class %s", className), e.getCause());
        }
    }

}
