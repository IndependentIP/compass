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

package org.compass.core.converter.mapping.osem;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.Iterator;

import org.compass.core.Property;
import org.compass.core.Resource;
import org.compass.core.accessor.Getter;
import org.compass.core.accessor.Setter;
import org.compass.core.converter.ConversionException;
import org.compass.core.converter.mapping.CollectionResourceWrapper;
import org.compass.core.converter.mapping.ResourceMappingConverter;
import org.compass.core.engine.SearchEngine;
import org.compass.core.engine.utils.ResourceHelper;
import org.compass.core.mapping.Mapping;
import org.compass.core.mapping.ResourceMapping;
import org.compass.core.mapping.ResourcePropertyMapping;
import org.compass.core.mapping.osem.ClassMapping;
import org.compass.core.mapping.osem.ClassPropertyMetaDataMapping;
import org.compass.core.mapping.osem.ObjectMapping;
import org.compass.core.mapping.osem.OsemMapping;
import org.compass.core.marshall.MarshallingContext;
import org.compass.core.marshall.MarshallingEnvironment;
import org.compass.core.spi.InternalResource;
import org.compass.core.spi.ResourceKey;
import org.compass.core.util.ClassUtils;

/**
 * @author kimchy
 */
public class ClassMappingConverter implements ResourceMappingConverter {

    public static final String ROOT_CLASS_MAPPING_KEY = "$rcmk";

    public static final String DISABLE_INTERNAL_MAPPINGS = "$dim";

    private static final Object DISABLE_INTERNAL_MAPPINGS_MARK = new Object();

    public boolean marshall(Resource resource, Object root, Mapping mapping, MarshallingContext context)
            throws ConversionException {

        // first store some important original context
        Object disableInternalMappings = context.getAttribute(DISABLE_INTERNAL_MAPPINGS);

        // perform the unmarshalling
        boolean store = doMarshall(resource, root, mapping, context);

        // restore the context
        context.setAttribute(DISABLE_INTERNAL_MAPPINGS, disableInternalMappings);

        return store;
    }

    protected boolean doMarshall(Resource resource, Object root, Mapping mapping, MarshallingContext context)
            throws ConversionException {
        SearchEngine searchEngine = context.getSearchEngine();
        ClassMapping classMapping = (ClassMapping) mapping;
        // Note that even if a component is root, it will not be root when
        // treated as a component (the binding part of the configuration takes
        // care and "unroots" it)
        if (classMapping.isRoot()) {
            doSetBoost(resource, root, classMapping, context);
            context.setAttribute(ROOT_CLASS_MAPPING_KEY, classMapping);
            context.setAttribute(DISABLE_INTERNAL_MAPPINGS, null);
        } else {
            if (!classMapping.isSupportUnmarshall()) {
                context.setAttribute(DISABLE_INTERNAL_MAPPINGS, DISABLE_INTERNAL_MAPPINGS_MARK);
            }
        }

        // handle null values
        if (root == null) {
            if (!classMapping.isSupportUnmarshall()) {
                return false;
            }
            if (!context.handleNulls()) {
                return false;
            }
            if (classMapping.getIdMappings().length == 0) {
                throw new ConversionException("Component mapping [" + classMapping.getAlias() +
                        "] used within a collection/array and has null value, in such cases please define at least one id mapping on it");
            }
            // go over all the ids and put a null value in it (just so we keep the order)
            ResourcePropertyMapping[] ids = classMapping.getIdMappings();
            boolean store = false;
            for (int i = 0; i < ids.length; i++) {
                store |= ids[i].getConverter().marshall(resource, context.getSearchEngine().getNullValue(), ids[i], context);
            }
            return store;
        }

        if (classMapping.isPoly() && classMapping.getPolyClass() == null) {
            // store the poly class only for root mappings when we don't support unmarshalling
            // and for all classes when we do support unmarshalling
            if (classMapping.isSupportUnmarshall() || classMapping.isRoot()) {
                storePolyClass(resource, root, searchEngine, classMapping);
            }
        }

        // check if we already marshalled this objecy under this alias
        // if we did, there is no need to completly marhsall it again
        // Note, this is only applicable for class with ids
        if (classMapping.getIdMappings().length > 0) {
            IdsAliasesObjectKey idObjKey = new IdsAliasesObjectKey(classMapping, root);
            if (!idObjKey.hasNullId) {
                Object marshalled = context.getMarshalled(idObjKey);
                if (marshalled != null) {
                    // we already marshalled this object, if we don't support unmarhsall, just return
                    // otherwise only marshall its ids and return
                    if (!classMapping.isSupportUnmarshall()) {
                        return true;
                    }
                    ResourcePropertyMapping[] ids = classMapping.getIdMappings();
                    boolean store = false;
                    for (int i = 0; i < ids.length; i++) {
                        store |= ids[i].getConverter().marshall(resource, idObjKey.idsValues[i], ids[i], context);
                    }
                    return store;
                } else {
                    // we did not marshall this object, cache it for later checks
                    context.setMarshalled(idObjKey, root);
                }
            }
        }

        // perform full marshalling of the object into the resource
        boolean store = false;
        for (Iterator mappingsIt = classMapping.mappingsIt(); mappingsIt.hasNext();) {
            context.setAttribute(MarshallingEnvironment.ATTRIBUTE_CURRENT, root);
            OsemMapping m = (OsemMapping) mappingsIt.next();
            Object value;
            if (m.hasAccessors()) {
                Getter getter = ((ObjectMapping) m).getGetter();
                value = getter.get(root);
            } else {
                value = root;
            }
            store |= m.getConverter().marshall(resource, value, m, context);
        }
        return store;
    }

    public Object unmarshall(Resource resource, Mapping mapping, MarshallingContext context) throws ConversionException {
        ClassMapping classMapping = (ClassMapping) mapping;
        ResourceKey resourceKey = null;
        // handle a cache of all the unmarshalled objects already, used for
        // cyclic references when using reference mappings or component mappings
        // with ids
        if (classMapping.isRoot()) {
            if (!classMapping.isSupportUnmarshall()) {
                // we don't support unmarshalling, try and unmarshall just the object with its
                // ids set.
                Object obj = constructObjectForUnmarshalling(classMapping, resource);
                ResourcePropertyMapping[] ids = classMapping.getIdMappings();
                for (int i = 0; i < ids.length; i++) {
                    Object idValue = ids[i].getConverter().unmarshall(resource, ids[i], context);
                    if (idValue == null) {
                        // was not marshalled, simply return null
                        return null;
                    }
                    ((ClassPropertyMetaDataMapping) ids[i]).getSetter().set(obj, idValue);
                }
                return obj;
            }
            resourceKey = ((InternalResource) resource).resourceKey();
            // if it is cached, return the cached object
            Object cached = context.getUnmarshalled(resourceKey);
            if (cached != null) {
                return cached;
            }
        } else if (classMapping.getIdMappings().length > 0) {
            // if the class mapping has ids, try and get it from the resource.
            Property[] propIds = ResourceHelper.toIds(resource, classMapping, false);
            if (propIds != null) {
                resourceKey = new ResourceKey(classMapping, propIds);
                // if it is cached, return the cached object
                Object cached = context.getUnmarshalled(resourceKey);
                if (cached != null) {
                    return cached;
                }
                // if we do have values in the ids, but all of them are null, it means that we
                // marked a null object
                boolean nullClass = true;
                for (int i = 0; i < propIds.length; i++) {
                    if (!context.getSearchEngine().isNullValue(propIds[i].getStringValue())) {
                        nullClass = false;
                    }
                }
                if (nullClass) {
                    return null;
                }
                // if it is not cached, we need to rollback the fact that we read
                // the ids, so the rest of the unmarshalling process will work
                if (resource instanceof CollectionResourceWrapper) {
                    CollectionResourceWrapper colWrapper = (CollectionResourceWrapper) resource;
                    ResourcePropertyMapping[] ids = classMapping.getIdMappings();
                    for (int i = 0; i < ids.length; i++) {
                        colWrapper.rollbackGetProperty(ids[i].getPath().getPath());
                    }
                }
            }
        }

        Object obj = constructObjectForUnmarshalling(classMapping, resource);
        context.setAttribute(MarshallingEnvironment.ATTRIBUTE_CURRENT, obj);
        // we will set here the object, even though no ids have been set,
        // since the ids are the first mappings that will be unmarshalled,
        // and it's all we need to handle cyclic refernces in case of
        // references or components with ids
        if (resourceKey != null) {
            context.setUnmarshalled(resourceKey, obj);
            if (classMapping.isRoot()) {
                context.getSession().getFirstLevelCache().set(resourceKey, obj);
            }
        }

        boolean isNullClass = true;
        for (Iterator mappingsIt = classMapping.mappingsIt(); mappingsIt.hasNext();) {
            context.setAttribute(MarshallingEnvironment.ATTRIBUTE_CURRENT, obj);
            OsemMapping m = (OsemMapping) mappingsIt.next();
            if (m.hasAccessors()) {
                Setter setter = ((ObjectMapping) m).getSetter();
                if (setter == null) {
                    continue;
                }
                Object value = m.getConverter().unmarshall(resource, m, context);
                if (value == null) {
                    continue;
                }
                setter.set(obj, value);
                if (m.controlsObjectNullability()) {
                    isNullClass = false;
                }
            } else {
                m.getConverter().unmarshall(resource, m, context);
            }
        }
        if (isNullClass) {
            return null;
        }
        return obj;
    }

    /**
     * Constructs the object used for unmarshalling (no properties are set/unmarshalled) on it.
     * <code>null</code> return value denotes no un-marshalling should be performed.
     */
    protected Object constructObjectForUnmarshalling(ClassMapping classMapping, Resource resource) throws ConversionException {
        // resolve the actual class and constructor
        Class clazz = classMapping.getClazz();
        Constructor constructor = classMapping.getConstructor();
        if (classMapping.isPoly()) {
            if (classMapping.getPolyClass() != null) {
                clazz = classMapping.getPolyClass();
                constructor = classMapping.getPolyConstructor();
            } else {
                Property pClassName = resource.getProperty(classMapping.getClassPath().getPath());
                if (pClassName == null) {
                    // if not poly class is stored, this means that it is probably a null class stored.
                    return null;
                }
                String className = pClassName.getStringValue();
                if (className == null) {
                    // if not poly class is stored, this means that it is probably a null class stored.
                    return null;
                }
                try {
                    clazz = ClassUtils.forName(className);
                } catch (ClassNotFoundException e) {
                    throw new ConversionException("Failed to create class [" + className + "] for unmarshalling", e);
                }
                constructor = ClassUtils.getDefaultConstructor(clazz);
            }
        }

        // create the object
        Object obj;
        try {
            obj = constructor.newInstance(null);
        } catch (Exception e) {
            throw new ConversionException("Failed to create class [" + clazz.getName() + "] for unmarshalling", e);
        }
        return obj;
    }

    public boolean marshallIds(Resource idResource, Object id, ResourceMapping resourceMapping, MarshallingContext context)
            throws ConversionException {
        ClassMapping classMapping = (ClassMapping) resourceMapping;
        boolean stored = false;
        ResourcePropertyMapping[] ids = classMapping.getIdMappings();
        if (classMapping.getClazz().isAssignableFrom(id.getClass())) {
            // the object is the key
            for (int i = 0; i < ids.length; i++) {
                ClassPropertyMetaDataMapping classPropertyMetaDataMapping = (ClassPropertyMetaDataMapping) ids[i];
                stored |= convertId(idResource, classPropertyMetaDataMapping.getGetter().get(id), classPropertyMetaDataMapping, context);
            }
        } else if (id.getClass().isArray()) {
            if (Array.getLength(id) != ids.length) {
                throw new ConversionException("Trying to load class with [" + Array.getLength(id)
                        + "] while has ids mappings of [" + ids.length + "]");
            }
            for (int i = 0; i < ids.length; i++) {
                stored |= convertId(idResource, Array.get(id, i), (ClassPropertyMetaDataMapping) ids[i], context);
            }
        } else if (ids.length == 1) {
            stored = convertId(idResource, id, (ClassPropertyMetaDataMapping) ids[0], context);
        } else {
            String type = id.getClass().getName();
            throw new ConversionException("Cannot marshall ids, not supported id object type [" + type
                    + "] and value [" + id + "], or you have not defined ids in the mapping files");
        }
        return stored;
    }

    private boolean convertId(Resource resource, Object root, ClassPropertyMetaDataMapping mdMapping, MarshallingContext context) {
        if (root == null) {
            throw new ConversionException("Trying to marshall a null id [" + mdMapping.getName() + "]");
        }
        return mdMapping.getConverter().marshall(resource, root, mdMapping, context);
    }

    public Object[] unmarshallIds(Object id, ResourceMapping resourceMapping, MarshallingContext context)
            throws ConversionException {
        ClassMapping classMapping = (ClassMapping) resourceMapping;
        ResourcePropertyMapping[] ids = classMapping.getIdMappings();
        Object[] idsValues = new Object[ids.length];
        if (id instanceof Resource) {
            Resource resource = (Resource) id;
            for (int i = 0; i < ids.length; i++) {
                idsValues[i] = ids[i].getConverter().unmarshall(resource, ids[i], context);
                if (idsValues[i] == null) {
                    // the reference was not marshalled
                    return null;
                }
            }
        } else if (classMapping.getClazz().isAssignableFrom(id.getClass())) {
            // the object is the key
            for (int i = 0; i < ids.length; i++) {
                ClassPropertyMetaDataMapping classPropertyMetaDataMapping = (ClassPropertyMetaDataMapping) ids[i];
                idsValues[i] = classPropertyMetaDataMapping.getGetter().get(id);
            }
        } else if (id.getClass().isArray()) {
            if (Array.getLength(id) != ids.length) {
                throw new ConversionException("Trying to load class with [" + Array.getLength(id)
                        + "] while has ids mappings of [" + ids.length + "]");
            }
            for (int i = 0; i < ids.length; i++) {
                idsValues[i] = Array.get(id, i);
            }
        } else if (ids.length == 1) {
            idsValues[0] = id;
        } else {
            String type = id.getClass().getName();
            throw new ConversionException("Cannot marshall ids, not supported id object type [" + type
                    + "] and value [" + id + "], or you have not defined ids in the mapping files");
        }
        return idsValues;
    }

    /**
     * A simple extension point that allows to set the boost value for the created {@link Resource}.
     * <p/>
     * The default implemenation uses the statically defined boost value in the mapping definition
     * ({@link org.compass.core.mapping.osem.ClassMapping#getBoost()}) to set the boost level
     * using {@link Resource#setBoost(float)}
     * <p/>
     * Note, that this method will only be called on a root level (root=true) mapping.
     *
     * @param resource     The resource to set the boost on
     * @param root         The Object that is marshalled into the respective Resource
     * @param classMapping The Class Mapping deifnition
     * @param context      The marshalling context
     * @throws ConversionException
     */
    protected void doSetBoost(Resource resource, Object root, ClassMapping classMapping,
                              MarshallingContext context) throws ConversionException {
        resource.setBoost(classMapping.getBoost());
    }

    /**
     * Stores the poly class name callback. Uses {@link #getPolyClassName(Object)} in order to get
     * the poly class and store it.
     *
     * @param resource     The resource to add the poly class to
     * @param root         The root class to write its class name
     * @param searchEngine The search engine to use
     * @param classMapping The class mapping to use
     */
    protected void storePolyClass(Resource resource, Object root, SearchEngine searchEngine, ClassMapping classMapping) {
        String className = getPolyClassName(root);
        Property p = searchEngine.createProperty(classMapping.getClassPath().getPath(), className, Property.Store.YES,
                Property.Index.UN_TOKENIZED);
        p.setOmitNorms(true);
        resource.addProperty(p);
    }

    /**
     * An extension point allowing to get the poly class name if need to be stored.
     * By defaults uses {@link Class#getName()}.
     */
    protected String getPolyClassName(Object root) {
        return root.getClass().getName();
    }

    /**
     * An object key based on the alias and the object identity hash code
     */
    protected static final class IdentityAliasedObjectKey {

        private String alias;

        private Integer objHashCode;

        private int hashCode = Integer.MIN_VALUE;

        public IdentityAliasedObjectKey(String alias, Object value) {
            this.alias = alias;
            this.objHashCode = new Integer(System.identityHashCode(value));
        }


        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            final IdentityAliasedObjectKey idObjKey = (IdentityAliasedObjectKey) other;
            return idObjKey.objHashCode.equals(this.objHashCode) && idObjKey.alias.equals(this.alias);
        }


        public int hashCode() {
            if (hashCode == Integer.MIN_VALUE) {
                hashCode = 13 * objHashCode.hashCode() + alias.hashCode();
            }
            return hashCode;
        }
    }

    /**
     * An object key based on the alias and its ids values
     */
    protected static final class IdsAliasesObjectKey {

        private String alias;

        private Object[] idsValues;

        private boolean hasNullId;

        private int hashCode = Integer.MIN_VALUE;

        public IdsAliasesObjectKey(ClassMapping classMapping, Object value) {
            this.alias = classMapping.getAlias();
            ResourcePropertyMapping[] ids = classMapping.getIdMappings();
            idsValues = new Object[ids.length];
            for (int i = 0; i < ids.length; i++) {
                OsemMapping m = (OsemMapping) ids[i];
                if (m.hasAccessors()) {
                    idsValues[i] = ((ObjectMapping) m).getGetter().get(value);
                } else {
                    idsValues[i] = value;
                }
                if (idsValues[i] == null) {
                    hasNullId = true;
                }
            }
        }

        public Object[] getIdsValues() {
            return this.idsValues;
        }

        public boolean equals(Object other) {
            if (this == other)
                return true;

//      We will make sure that it never happens
//        if (!(other instanceof IdsAliasesObjectKey))
//            return false;

            final IdsAliasesObjectKey key = (IdsAliasesObjectKey) other;
            if (!key.alias.equals(alias)) {
                return false;
            }

            for (int i = 0; i < idsValues.length; i++) {
                if (!key.idsValues[i].equals(idsValues[i])) {
                    return false;
                }
            }

            return true;
        }

        public int hashCode() {
            if (hashCode == Integer.MIN_VALUE) {
                hashCode = getHashCode();
            }
            return hashCode;
        }

        private int getHashCode() {
            int result = alias.hashCode();
            for (int i = 0; i < idsValues.length; i++) {
                result = 29 * result + idsValues[i].hashCode();
            }
            return result;
        }

        public boolean isHasNullId() {
            return hasNullId;
        }
    }
}
