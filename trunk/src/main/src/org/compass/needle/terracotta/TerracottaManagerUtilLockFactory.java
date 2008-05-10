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

package org.compass.needle.terracotta;

import java.io.IOException;
import java.util.UUID;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.lockmanager.api.LockLevel;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.LockFactory;

/**
 * A Terracotta {@link com.tc.object.bytecode.ManagerUtil} based lock factory.
 *
 * <p>Currently disabled since locking does not seem to work.
 *
 * @author kimchy
 */
public class TerracottaManagerUtilLockFactory extends LockFactory {

    private final String lockPrefix;

    public TerracottaManagerUtilLockFactory() {
        this(UUID.randomUUID().toString());
    }

    public TerracottaManagerUtilLockFactory(String lockPrefix) {
        this.lockPrefix = lockPrefix;
    }

    public Lock makeLock(String lockName) {
        return new TerracottaManagerUtilLock(lockPrefix + "-" + lockName);
    }

    public void clearLock(String lockName) throws IOException {
        if (ManagerUtil.isLocked(lockName, LockLevel.WRITE)) {
            ManagerUtil.commitLock(lockName);
        }
    }
}