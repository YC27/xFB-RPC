/* Licensed to the xFB-RPC under one or more
 * contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The xFB-RPC licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ysc.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Configuration {

  private static final Logger LOGGER = LoggerFactory.getLogger(Configuration.class);

  private static final String CONFIG_FILE_NAME = "application.properties";

  public Configuration() {
    loadProps();
  }

  private void loadProps() {
    final Optional<URL> url = getPropsUrl();
    if (url.isPresent()) {
      try (final InputStream inputStream =
          Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE_NAME)) {
        LOGGER.info("Start to read config file {}", url.get());
        final Properties properties = new Properties();
        assert inputStream != null;
        properties.load(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        final TrimProperties trimProperties = new TrimProperties();
        trimProperties.putAll(properties);
        load(trimProperties);
      } catch (final FileNotFoundException e) {
        LOGGER.error("Fail to find config file, reject startup.", e);
        System.exit(-1);
      } catch (final IOException e) {
        LOGGER.error("IO exception when reading config file, reject startup.", e);
        System.exit(-1);
      } catch (final Exception e) {
        LOGGER.error("Unexpected exception when reading config file, reject startup.", e);
        System.exit(-1);
      }
    } else {
      LOGGER.warn("{} is not found, use default configuration", CONFIG_FILE_NAME);
    }
  }

  public abstract void load(final TrimProperties properties);

  private Optional<URL> getPropsUrl() {
    try {
      final String urlString = "file:" + CONFIG_FILE_NAME;

      return Optional.of(new URL(urlString));
    } catch (final MalformedURLException e) {
      LOGGER.warn("get url failed", e);
      return Optional.empty();
    }
  }
}
