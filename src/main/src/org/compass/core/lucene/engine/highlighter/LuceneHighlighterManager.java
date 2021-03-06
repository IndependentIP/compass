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

package org.compass.core.lucene.engine.highlighter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.util.ClassUtils;

/**
 * 
 * @author kimchy
 * 
 */
public class LuceneHighlighterManager {

    private static final Log log = LogFactory.getLog(LuceneHighlighterManager.class);

    private LuceneHighlighterSettings defaultHighlighterSettings;

    private HashMap highlightersSettings = new HashMap();

    public void configure(CompassSettings settings) throws SearchEngineException {
        Map highlighterSettingGroups = settings.getSettingGroups(LuceneEnvironment.Highlighter.PREFIX);

        for (Iterator it = highlighterSettingGroups.keySet().iterator(); it.hasNext();) {
            String highlighterName = (String) it.next();
            if (log.isInfoEnabled()) {
                log.info("Building highlighter [" + highlighterName + "]");
            }
            LuceneHighlighterSettings highlighter = buildHighlighter(highlighterName,
                    (CompassSettings) highlighterSettingGroups.get(highlighterName));
            highlightersSettings.put(highlighterName, highlighter);
        }
        defaultHighlighterSettings = (DefaultLuceneHighlighterSettings) highlightersSettings
                .get(LuceneEnvironment.Highlighter.DEFAULT_GROUP);
        if (defaultHighlighterSettings == null) {
            // if no default highlighter is defined, we need to configre one
            defaultHighlighterSettings = buildHighlighter(LuceneEnvironment.Highlighter.DEFAULT_GROUP,
                    new CompassSettings());
            highlightersSettings.put(LuceneEnvironment.Highlighter.DEFAULT_GROUP, defaultHighlighterSettings);
        }
    }

    private LuceneHighlighterSettings buildHighlighter(String highlighterName, CompassSettings settings) {
        String highlighterFactorySetting = settings.getSetting(LuceneEnvironment.Highlighter.FACTORY, null);
        LuceneHighlighterFactory highlighterFactory;
        if (highlighterFactorySetting == null) {
            highlighterFactory = new DefaultLuceneHighlighterFactory();
        } else {
            try {
                highlighterFactory = (LuceneHighlighterFactory) ClassUtils.forName(highlighterFactorySetting)
                        .newInstance();
            } catch (Exception e) {
                throw new SearchEngineException("Cannot create Highlighter factory [" + highlighterFactorySetting
                        + "]. Please verify the highlighter factory setting at ["
                        + LuceneEnvironment.Highlighter.FACTORY + "]", e);
            }
        }
        return highlighterFactory.createHighlighterSettings(highlighterName, settings);
    }

    public LuceneHighlighterSettings getDefaultHighlighterSettings() {
        return defaultHighlighterSettings;
    }

    public LuceneHighlighterSettings getHighlighterSettings(String highlighterName) {
        return (LuceneHighlighterSettings) highlightersSettings.get(highlighterName);
    }

    public LuceneHighlighterSettings getHighlighterSettingsMustExists(String highlighterName) {
        LuceneHighlighterSettings highlighterSettings = (LuceneHighlighterSettings) highlightersSettings
                .get(highlighterName);
        if (highlighterSettings == null) {
            throw new SearchEngineException("No highlighter is defined for highlighter name [" + highlighterName + "]");
        }
        return highlighterSettings;
    }

}
