/*
 * Copyright 2004-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.compass.core.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.compass.core.util.ClassUtils;
import org.compass.core.util.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * A set of settings that are used to configure the Compass instance.
 *
 * @author kimchy
 */
public class CompassSettings {

    private Properties settings;

    private final Map groups = new ConcurrentHashMap();

    private Map registry = new ConcurrentHashMap();

    public CompassSettings() {
        this(new Properties());
    }

    public CompassSettings(Properties settings) {
        this.settings = settings;
    }

    public void addSettings(Properties settings) {
        this.settings.putAll(settings);
    }

    public void addSettings(CompassSettings settings) {
        this.settings.putAll(settings.getProperties());
    }

    public CompassSettings copy() {
        CompassSettings copySettings = new CompassSettings((Properties) settings.clone());
        copySettings.registry = new ConcurrentHashMap(registry);
        return copySettings;
    }

    public Properties getProperties() {
        return settings;
    }

    public Collection keySet() {
        return settings.keySet();
    }

    public String getSetting(String setting) {
        return settings.getProperty(setting);
    }

    public String getSetting(String setting, String defaultValue) {
        return settings.getProperty(setting, defaultValue);
    }

    public Map getSettingGroups(String settingPrefix) {
        if (settingPrefix.charAt(settingPrefix.length() - 1) != '.') {
            settingPrefix = settingPrefix + ".";
        }
        Map group = (Map) groups.get(settingPrefix);
        if (group != null) {
            return group;
        }
        // we don't really care that it might happen twice
        HashMap map = new HashMap();
        for (Iterator it = settings.keySet().iterator(); it.hasNext();) {
            String setting = (String) it.next();
            if (setting.startsWith(settingPrefix)) {
                String nameValue = setting.substring(settingPrefix.length());
                int dotIndex = nameValue.indexOf('.');
                if (dotIndex == -1) {
                    throw new ConfigurationException("Failed to get setting group for [" + settingPrefix
                            + "] setting prefix and setting [" + setting + "] because of a missing '.'");
                }
                String name = nameValue.substring(0, dotIndex);
                String value = nameValue.substring(dotIndex + 1);
                CompassSettings groupSettings = (CompassSettings) map.get(name);
                if (groupSettings == null) {
                    groupSettings = new CompassSettings();
                    map.put(name, groupSettings);
                }
                groupSettings.setSetting(value, getSetting(setting));
            }
        }
        groups.put(settingPrefix, map);
        return map;
    }

    public float getSettingAsFloat(String setting, float defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Float.parseFloat(sValue);
    }

    public double getSettingAsDouble(String setting, double defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Double.parseDouble(sValue);
    }

    public int getSettingAsInt(String setting, int defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Integer.parseInt(sValue);
    }

    public long getSettingAsLong(String setting, long defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Long.parseLong(sValue);
    }

    public boolean getSettingAsBoolean(String setting, boolean defaultValue) {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return defaultValue;
        }
        return Boolean.valueOf(sValue).booleanValue();
    }

    public Class getSettingAsClass(String setting, Class clazz) throws ClassNotFoundException {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return clazz;
        }
        return ClassUtils.forName(sValue);
    }

    public Class getSettingAsClass(String setting, Class clazz, ClassLoader classLoader) throws ClassNotFoundException {
        String sValue = getSetting(setting);
        if (sValue == null) {
            return clazz;
        }
        return ClassUtils.forName(sValue, classLoader);
    }

    public CompassSettings setSetting(String setting, String value) {
        if (value == null) {
            return this;
        }
        this.settings.setProperty(setting, value);
        return this;
    }

    public CompassSettings setBooleanSetting(String setting, boolean value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setFloatSetting(String setting, float value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setDoubleSetting(String setting, double value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setIntSetting(String setting, int value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setLongSetting(String setting, long value) {
        setSetting(setting, String.valueOf(value));
        return this;
    }

    public CompassSettings setClassSetting(String setting, Class clazz) {
        setSetting(setting, clazz.getName());
        return this;
    }

    /**
     * Sets a group of settings, sharing the same setting prefix. The provided
     * settings are appended to the settingPrefix, and the matching values are
     * set.
     * <p/>
     * The constructed setting is: settingPrefix + "." + groupName + "." + settings[i].
     *
     * @param settingPrefix The prefix used for all settings
     * @param groupName     The name of the setting group
     * @param settings      The settings name appended to settingsPrefix + "." + groupName + "."
     * @param values        The values of the settings matched against the settings parameters
     * @return This settings instance for method chaining
     */
    public CompassSettings setGroupSettings(String settingPrefix, String groupName, String[] settings, String[] values) {
        if (settings.length != values.length) {
            throw new IllegalArgumentException("The settings length must match the value length");
        }
        for (int i = 0; i < settings.length; i++) {
            if (values[i] == null) {
                continue;
            }
            setSetting(settingPrefix + "." + groupName + "." + settings[i], values[i]);
        }
        return this;
    }

    /**
     * ADANCE: An internal compass global registry
     */
    public Object getRegistry(Object key) {
        return registry.get(key);
    }

    /**
     * ADVANCE: An internal compass global registry
     */
    public void setRegistry(Object key, Object value) {
        registry.put(key, value);
    }

    /**
     * ADVANCE: An internal compass global registry
     */
    public Object removeRegistry(Object key) {
        return registry.remove(key);
    }

    public String toString() {
        return settings.toString();
    }
}
