package org.compass.core.lucene.engine.indexdeletionpolicy;

import org.apache.lucene.index.IndexDeletionPolicy;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.store.Directory;
import org.compass.core.CompassException;
import org.compass.core.config.CompassConfigurable;
import org.compass.core.config.CompassSettings;
import org.compass.core.engine.SearchEngineException;
import org.compass.core.lucene.LuceneEnvironment;
import org.compass.core.util.ClassUtils;

/**
 * An {@link org.apache.lucene.index.IndexDeletionPolicy} factory.
 *
 * @author kimchy
 */
public class IndexDeletionPolicyFactory implements CompassConfigurable {

    private CompassSettings settings;

    private String indexDeletionPolicyType;

    private IndexDeletionPolicy globalIndexDeletionPolicy;

    public void configure(CompassSettings settings) throws CompassException {
        this.settings = settings;
        indexDeletionPolicyType = settings.getSetting(LuceneEnvironment.IndexDeletionPolicy.TYPE, LuceneEnvironment.IndexDeletionPolicy.KeepLastCommit.NAME);
        if (LuceneEnvironment.IndexDeletionPolicy.KeepLastCommit.NAME.equalsIgnoreCase(indexDeletionPolicyType)) {
            globalIndexDeletionPolicy = new KeepOnlyLastCommitDeletionPolicy();
        } else if (LuceneEnvironment.IndexDeletionPolicy.KeepLastN.NAME.equalsIgnoreCase(indexDeletionPolicyType)) {
            globalIndexDeletionPolicy = new KeepLastNDeletionPolicy();
        } else if (LuceneEnvironment.IndexDeletionPolicy.KeepAll.NAME.equalsIgnoreCase(indexDeletionPolicyType)) {
            globalIndexDeletionPolicy = new KeepAllDeletionPolicy();
        } else
        if (LuceneEnvironment.IndexDeletionPolicy.KeepNoneOnInit.NAME.equalsIgnoreCase(indexDeletionPolicyType)) {
            globalIndexDeletionPolicy = new KeepNoneOnInitDeletionPolicy();
        }
        if (globalIndexDeletionPolicy != null) {
            if (globalIndexDeletionPolicy instanceof CompassConfigurable) {
                ((CompassConfigurable) globalIndexDeletionPolicy).configure(settings);
            }
        }
    }

    public IndexDeletionPolicy createIndexDeletionPolicy(Directory dir) throws SearchEngineException {
        if (globalIndexDeletionPolicy != null) {
            return globalIndexDeletionPolicy;
        }
        if (LuceneEnvironment.IndexDeletionPolicy.ExpirationTime.NAME.equalsIgnoreCase(indexDeletionPolicyType)) {
            ExpirationTimeDeletionPolicy indexDeletionPolicy = new ExpirationTimeDeletionPolicy();
            indexDeletionPolicy.setDirectory(dir);
            indexDeletionPolicy.configure(settings);
            return indexDeletionPolicy;
        }
        try {
            IndexDeletionPolicy indexDeletionPolicy = (IndexDeletionPolicy) ClassUtils.forName(indexDeletionPolicyType).newInstance();
            if (indexDeletionPolicy instanceof DirectoryConfigurable) {
                ((DirectoryConfigurable) indexDeletionPolicy).setDirectory(dir);
            }
            if (indexDeletionPolicy instanceof CompassConfigurable) {
                ((CompassConfigurable) indexDeletionPolicy).configure(settings);
            }
            return indexDeletionPolicy;
        } catch (Exception e) {
            throw new SearchEngineException("Failed to create custom index deletion policy [" + indexDeletionPolicyType + "]", e);
        }
    }
}
