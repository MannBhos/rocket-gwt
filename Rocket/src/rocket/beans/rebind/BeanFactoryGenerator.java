/*
 * Copyright Miroslav Pokorny
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rocket.beans.rebind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import rocket.beans.client.FactoryBean;
import rocket.beans.rebind.beanreference.BeanReference;
import rocket.beans.rebind.constructor.ConstructorTemplatedFile;
import rocket.beans.rebind.deferredbinding.DeferredBinding;
import rocket.beans.rebind.factorymethod.FactoryMethodTemplatedFile;
import rocket.beans.rebind.init.CustomTemplatedFile;
import rocket.beans.rebind.list.ListValue;
import rocket.beans.rebind.map.MapValue;
import rocket.beans.rebind.properties.SetPropertiesTemplatedFile;
import rocket.beans.rebind.registerbeans.BuildFactoryBeansTemplatedFile;
import rocket.beans.rebind.set.SetValue;
import rocket.beans.rebind.stringvalue.StringValue;
import rocket.beans.rebind.value.Value;
import rocket.beans.rebind.xml.BeanFactoryDtdEntityResolver;
import rocket.beans.rebind.xml.BeanReferenceTag;
import rocket.beans.rebind.xml.BeanTag;
import rocket.beans.rebind.xml.DocumentWalker;
import rocket.beans.rebind.xml.ListTag;
import rocket.beans.rebind.xml.MapTag;
import rocket.beans.rebind.xml.PropertyTag;
import rocket.beans.rebind.xml.RemoteJsonServiceTag;
import rocket.beans.rebind.xml.RemoteRpcServiceTag;
import rocket.beans.rebind.xml.RethrowSaxExceptionsErrorHandler;
import rocket.beans.rebind.xml.SetTag;
import rocket.beans.rebind.xml.StringTag;
import rocket.beans.rebind.xml.ValueTag;
import rocket.generator.rebind.Generator;
import rocket.generator.rebind.GeneratorContext;
import rocket.generator.rebind.GeneratorHelper;
import rocket.generator.rebind.Visibility;
import rocket.generator.rebind.constructor.Constructor;
import rocket.generator.rebind.constructorparameter.ConstructorParameter;
import rocket.generator.rebind.method.Method;
import rocket.generator.rebind.method.NewMethod;
import rocket.generator.rebind.methodparameter.MethodParameter;
import rocket.generator.rebind.methodparameter.NewMethodParameter;
import rocket.generator.rebind.type.NewConcreteType;
import rocket.generator.rebind.type.NewNestedType;
import rocket.generator.rebind.type.NewType;
import rocket.generator.rebind.type.Type;
import rocket.generator.rebind.visitor.TypeConstructorsVisitor;
import rocket.generator.rebind.visitor.VirtualMethodVisitor;
import rocket.util.client.ObjectHelper;
import rocket.util.client.StringHelper;

/**
 * This code generator generates a BeanFactory which will create or provide the
 * beans defined in an xml file.
 * 
 * @author Miroslav Pokorny
 */
public class BeanFactoryGenerator extends Generator {

	public BeanFactoryGenerator() {
		this.setBeans(this.createBeans());
	}

	/**
	 * Prepares to transform a xml file into a BeanFactory with beans.
	 * 
	 * @param type The type passed to GWT.create() 
	 * @param newTypeName The name of the type being generated.
	 */
	protected NewConcreteType assembleNewType(final Type type, final String newTypeName) {
		this.verifyIsABeanFactory(type);

		final DocumentWalker document = this.getDocumentWalker(type);

		final NewConcreteType beanFactory = this.createBeanFactory(newTypeName);
		this.setBeanFactory(beanFactory);

		final List beanTags = document.getBeanElements();
		this.buildFactoryBeans(beanTags);

		this.overrideAllFactoryBeanCreateInstances(beanTags);
		this.overrideAllFactoryBeanSatisfyInits(beanTags);
		this.overrideAllFactoryBeanSatisfyProperties(beanTags);

		this.buildRemoteJsonServiceFactoryBeans(document.getRemoteJsonElements());
		this.buildRemoteRpcServiceFactoryBeans(document.getRemoteRpcElements());

		this.overrideBeanFactoryBuildFactoryBeans();

		return beanFactory;
	}

	/**
	 * Factory method which creates a walker that may be used to walk around the document.
	 * @param type
	 * @return
	 */
	protected DocumentWalker getDocumentWalker(final Type type) {
		final String fileName = this.getResourceName(type, Constants.BEAN_FILE_SUFFIX);

		final DocumentWalker document = new DocumentWalker();
		document.setEntityResolver(new BeanFactoryDtdEntityResolver());
		document.setErrorHandler(new RethrowSaxExceptionsErrorHandler());
		document.prepare(this, fileName);
		return document;
	}

	/**
	 * Verifies and throws an exception if the given type is a BeanFactory
	 * @param The bean type interface
	 */
	protected void verifyIsABeanFactory(final Type type) {
		ObjectHelper.checkNotNull("parameter:type", type);

		this.getGeneratorContext().info("Verifying " + type + " is a bean factory.");

		final Type beanFactory = this.getBeanFactoryType();
		if (false == type.isAssignableTo(beanFactory)) {
			throwNotABeanFactory("The interface type [" + type + "] is not a " + beanFactory);
		}
	}

	protected void throwNotABeanFactory(final String message) {
		throw new BeanFactoryGeneratorException(message);
	}

	/**
	 * Factory method which creates a new BeanFactory read to accept methods and fields etc.  
	 * @param newTypeName
	 * @return
	 */
	protected NewConcreteType createBeanFactory(final String newTypeName) {
		this.getGeneratorContext().info("Creating BeanFactory with a name of [" + newTypeName + "].");

		final NewConcreteType beanFactory = this.getGeneratorContext().newConcreteType();

		beanFactory.setAbstract(false);
		beanFactory.setFinal(true);
		beanFactory.setName(newTypeName);
		beanFactory.setSuperType(this.getBeanFactoryImpl());

		return beanFactory;
	}

	/**
	 * Walks all the bean tags building a NewNestedType FactoryBean for each tag encountered.
	 * @param beans
	 */
	protected void buildFactoryBeans(final List beans) {
		ObjectHelper.checkNotNull("parameter:beans", beans);

		this.getGeneratorContext().info("Creating FactoryBean's for each of the bean (" + beans.size() + ") tags.");

		final Iterator iterator = beans.iterator();
		while (iterator.hasNext()) {
			this.createBeanFactoryBean((BeanTag) iterator.next());
		}
	}

	protected void createBeanFactoryBean(final BeanTag beanTag) {
		ObjectHelper.checkNotNull("parameter:bean", beanTag);

		final Bean bean = new Bean();
		final String id = beanTag.getId();
		bean.setId(id);

		final String className = beanTag.getClassName();
		final Type beanType = this.getConcreteType(id, className);
		bean.setType(beanType);

		final Type superType = beanTag.isSingleton() ? this.getSingletonFactoryBean() : this.getPrototypeFactoryBean();
		final NewNestedType factoryBean = this.getBeanFactory().newNestedType();
		factoryBean.setStatic(false);
		factoryBean.setName(id + Constants.FACTORY_BEAN_SUFFIX);
		factoryBean.setSuperType(superType);
		factoryBean.setVisibility(Visibility.PRIVATE);
		bean.setFactoryBean(factoryBean);

		this.getGeneratorContext().debug("Subclassing " + superType + " as the FactoryBean for bean: " + bean);

		this.addBean(id, bean);
	}
	
	/**
	 * Visit all defined beans and overrides the createInstance of the factory bean.
	 * 
	 * A constructor with parameters or factory bean may be the source of the new bean instance.
	 * @param bean the beans element
	 */
	protected void overrideAllFactoryBeanCreateInstances(final List beans) {
		ObjectHelper.checkNotNull("parameter:beans", beans);

		this.getGeneratorContext().info("Overriding createInstance method for " + beans.size() + " bean(s).");

		final Iterator iterator = beans.iterator();
		while (iterator.hasNext()) {
			this.overrideFactoryBeanCreateInstance((BeanTag) iterator.next());
		}
	}

	/**
	 * Depending on the bean tag overrides the appropriate {@link FactoryBean#createInstance()} with
	 * an inline constructor call or inserts a call to a static factory method.
	 * @param beanTag The bean
	 */
	protected void overrideFactoryBeanCreateInstance(final BeanTag beanTag) {
		ObjectHelper.checkNotNull("parameter:bean", beanTag);

		final String factoryMethodName = beanTag.getFactoryMethod();
		final Bean bean = this.getBean(beanTag.getId());

		if (StringHelper.isNullOrEmpty(factoryMethodName)) {
			final List constructorArguments = beanTag.getConstructorArguments();
			this.overrideFactoryBeanCreateInstanceNewConstructor(bean, constructorArguments);
		} else {
			this.overrideFactoryBeanCreateInstanceFactoryMethod(bean, factoryMethodName);
		}
	}

	/**
	 * Override the createInstance method of the given factory bean to invoke a constructor
	 * @param bean The bean 
	 * @param constructorArguments a list containing the constructor arguments
	 */
	protected void overrideFactoryBeanCreateInstanceNewConstructor(final Bean bean, final List constructorArguments) {
		ObjectHelper.checkNotNull("parameter:bean", bean);
		ObjectHelper.checkNotNull("parameter:constructorArguments", constructorArguments);

		this.getGeneratorContext().debug(
				"Inserting statement to call constructor (with " + constructorArguments.size()
						+ " argument(s)) in FactoryBean.createInstance for bean " + bean);

		final NewMethod newFactoryMethod = this.createCreateInstanceMethod(bean.getFactoryBean());

		final ConstructorTemplatedFile body = new ConstructorTemplatedFile();
		newFactoryMethod.setBody(body);

		// make a list of values from constructorArguments.
		final List constructorValues = new ArrayList();
		final Iterator argumentIterator = constructorArguments.iterator();
		while (argumentIterator.hasNext()) {
			final Value value = this.asValue((ValueTag) argumentIterator.next());
			constructorValues.add(value);
			body.addArgument(value);
		}

		final List matchingConstructors = new ArrayList();
		final TypeConstructorsVisitor visitor = new TypeConstructorsVisitor() {

			protected boolean visit(final Constructor constructor) {
				boolean match = false;

				// skip non public constructors				
				if (constructor.getVisibility() == Visibility.PUBLIC) {
					final List constructorParameters = constructor.getParameters();

					// skip the given constructor with different numbers of parameters.
					if (constructorParameters.size() == constructorValues.size()) {

						match = true;
						final Iterator valuesIterator = constructorValues.iterator();
						final Iterator parametersIterator = constructorParameters.iterator();

						while (valuesIterator.hasNext()) {
							final ConstructorParameter parameter = (ConstructorParameter) parametersIterator.next();
							final Value value0 = (Value) valuesIterator.next();
							if (false == value0.isCompatibleWith(parameter.getType())) {
								match = false;
							}
						}
					}
				}

				if (match) {
					matchingConstructors.add(constructor);
				}
				return false;
			}
		};

		final Type beanType = bean.getType();
		visitor.start(beanType);

		if (matchingConstructors.size() == 0) {
			this.throwUnableToFindConstructor(bean);
		}
		if (matchingConstructors.size() > 1) {
			this.throwTooManyConstructors(bean, matchingConstructors);
		}

		final Constructor constructor = (Constructor) matchingConstructors.get(0);
		body.setBean(constructor);

		final Iterator constructorParameters = constructor.getParameters().iterator();
		final Iterator valuesIterator = constructorValues.iterator();
		while (constructorParameters.hasNext()) {
			final ConstructorParameter constructorParameter = (ConstructorParameter) constructorParameters.next();
			final Value value = (Value) valuesIterator.next();
			value.setType(constructorParameter.getType());
		}
	}

	/**
	 * This method throws an exception when more than one constructor satisfies the specified values for a bean type
	 * @param bean
	 * @param matchingConstructors A list containing the matching constructors.
	 */
	protected void throwTooManyConstructors(final Bean bean, final List matchingConstructors) {
		throw new BeanFactoryGeneratorException("Found more than one constructor that matches the specified constructor arguments, bean: "
				+ bean);
	}

	/**
	 * This method throws an exception when a suitable constructor cannot be found that matches the bean's constructor parameters or lack of them.
	 * @param bean
	 */
	protected void throwUnableToFindConstructor(final Bean bean) {
		throw new BeanFactoryGeneratorException(
				"Unable to find a constructor that is suitable for the specified constructor arguments for bean: " + bean);
	}

	/**
	 * Overrides the createInstance method of the given factory bean to invoke a static factory method.
	 * @param bean A bean definition
	 * @param factoryMethodName A fully qualified class and factory method
	 */
	protected void overrideFactoryBeanCreateInstanceFactoryMethod(final Bean bean, final String factoryMethodName) {
		ObjectHelper.checkNotNull("parameter:bean", bean);
		StringHelper.checkNotEmpty("parameter:factoryMethodName", factoryMethodName);

		final Type beanType = bean.getType();
		final Method factoryMethod = beanType.findMethod(factoryMethodName, Collections.EMPTY_LIST);
		if (null == factoryMethod || false == factoryMethod.isStatic() || factoryMethod.getVisibility() != Visibility.PUBLIC) {
			this.throwFactoryMethodNotFound(bean, factoryMethodName);
		}

		this.getGeneratorContext()
				.debug("Inserting statement in FactoryBean.createInstance to call " + factoryMethod + " for bean " + bean);

		final NewMethod newFactoryMethod = this.createCreateInstanceMethod(bean.getFactoryBean());

		final FactoryMethodTemplatedFile body = new FactoryMethodTemplatedFile();
		body.setFactoryType(beanType);
		body.setFactoryMethod(factoryMethod);

		newFactoryMethod.setBody(body);
	}

	protected void throwFactoryMethodNotFound(final Bean bean, final String method) {
		throw new BeanFactoryGeneratorException("Unable to find a public static method factory method [" + method + "] on " + bean);
	}

	protected NewMethod createCreateInstanceMethod(final NewType factoryBean) {
		ObjectHelper.checkNotNull("parameter:factoryBean", factoryBean);

		final Method method = factoryBean.getMostDerivedMethod("createInstance", Collections.EMPTY_LIST);
		return method.copy(factoryBean);
	}

	/**
	 * Visit all defined beans and implement the satisfyInit for each factory to set the properties.
	 * @param beans A list of beans
	 */
	protected void overrideAllFactoryBeanSatisfyInits(final List beans) {
		ObjectHelper.checkNotNull("parameter:beans", beans);

		this.getGeneratorContext().info("Overriding satisfyInit method for " + beans.size() + " bean(s).");

		final Iterator iterator = beans.iterator();
		while (iterator.hasNext()) {
			final BeanTag beanTag = (BeanTag) iterator.next();
			final String initMethodName = beanTag.getInitMethod();

			if (false == StringHelper.isNullOrEmpty(initMethodName)) {
				final Bean bean = this.getBean(beanTag.getId());
				this.overrideFactoryBeanSatisfyInit(bean, initMethodName);
			}
		}
	}

	/**
	 * Overrides the satisfyInit method for the factory bean to call the init method of the enclosed bean
	 * @param bean
	 * @param initMethodName
	 */
	protected void overrideFactoryBeanSatisfyInit(final Bean bean, final String initMethodName) {
		ObjectHelper.checkNotNull("parameter:bean", bean);
		GeneratorHelper.checkJavaMethodName("parameter:initMethodName", initMethodName);

		final Type beanType = bean.getType();
		final Method initMethod = beanType.findMethod(initMethodName, Collections.EMPTY_LIST);
		if (null == initMethod || initMethod.isStatic() || initMethod.getVisibility() != Visibility.PUBLIC) {
			throwInitMethodNotFound(bean, initMethodName);
		}

		final GeneratorContext context = this.getGeneratorContext();
		context.debug("Overriding satisfyInit to call " + initMethod + " for bean: " + bean);

		final NewType beanFactory = bean.getFactoryBean();
		final Method beanFactoryInitMethod = beanFactory.getMostDerivedMethod( Constants.SATISFY_INIT, this.getParameterListWithOnlyObject() );
		final NewMethod newMethod = beanFactoryInitMethod.copy(beanFactory);

		final CustomTemplatedFile body = new CustomTemplatedFile();
		body.setBean(beanType);
		body.setCustomMethod(initMethod);
		final NewMethodParameter instance = (NewMethodParameter) newMethod.getParameters().get(0);
		body.setInstance(instance);

		newMethod.setBody(body);
	}

	/**
	 * This method throws an exception when the specified init method does not exist, is not public or is static.
	 * @param bean
	 * @param initMethodName
	 */
	protected void throwInitMethodNotFound(final Bean bean, final String initMethodName) {
		throw new BeanFactoryGeneratorException("Unable to find a public method called [" + initMethodName + "] on the bean " + bean);
	}

	/**
	 * Visit all defined beans and implement the satisfyProperties for each factory to set the properties.
	 * @param beans A list of beans
	 */
	protected void overrideAllFactoryBeanSatisfyProperties(final List beans) {
		ObjectHelper.checkNotNull("parameter:beans", beans);

		this.getGeneratorContext().info("Overriding satisfyProperties method for " + beans.size() + " bean(s).");

		final Iterator iterator = beans.iterator();
		while (iterator.hasNext()) {
			final BeanTag beanTag = (BeanTag) iterator.next();

			final String id = beanTag.getId();
			final Bean bean = BeanFactoryGenerator.this.getBean(id);
			final List properties = beanTag.getProperties();

			this.overrideFactoryBeanSatisfyProperties(bean, properties);
		}
	}

	/**
	 * Overrides the abstract satisfyProperties with statements that set each property. 
	 * @param bean
	 * @param properties
	 */
	protected void overrideFactoryBeanSatisfyProperties(final Bean bean, final List properties) {
		ObjectHelper.checkNotNull("parameter:bean", bean);
		ObjectHelper.checkNotNull("parameter:properties", properties);

		final GeneratorContext context = this.getGeneratorContext();
		context.debug("Attempting to create a method which will set " + properties.size() + " properties upon " + bean);

		final Type voidType = this.getGeneratorContext().getVoid();
		final Type beanType = bean.getType();

		final SetPropertiesTemplatedFile body = new SetPropertiesTemplatedFile();
		body.setBean(beanType);

		final NewType factoryBean = bean.getFactoryBean();
		final Method method = factoryBean.getMostDerivedMethod( Constants.SATISFY_PROPERTIES, this.getParameterListWithOnlyObject() );
		final NewMethod newMethod = method.copy(factoryBean);
		body.setInstance((MethodParameter) newMethod.getParameters().get(0));
		newMethod.setBody(body);

		// loop thru all properties
		final Iterator propertyIterator = properties.iterator();
		while (propertyIterator.hasNext()) {
			final PropertyTag propertyTag = (PropertyTag) propertyIterator.next();
			final String propertyName = propertyTag.getPropertyName();
			final String setterName = GeneratorHelper.buildSetterName(propertyName);

			context.debug("Searching for setter method [" + setterName + "] for property upon bean " + bean);

			final Value value = this.asValue(propertyTag.getValue());
			final List matching = new ArrayList();

			final VirtualMethodVisitor visitor = new VirtualMethodVisitor() {
				protected boolean visit(final Method method) {

					while (true) {
						// names dont match
						if (false == method.getName().equals(setterName)) {
							break;
						}
						// return type must be void
						if (false == method.getReturnType().equals(voidType)) {
							break;
						}
						// parameter types must be compatible...
						final List parameters = method.getParameters();
						if (parameters.size() != 1) {
							break;
						}

						final MethodParameter parameter = (MethodParameter) parameters.get(0);
						final Type propertyType = parameter.getType();
						if (false == value.isCompatibleWith(propertyType)) {
							break;
						}
						value.setType(propertyType);
						matching.add(method);
						break;
					}

					return false;
				}

				protected boolean skipJavaLangObjectMethods() {
					return true;
				}
			};

			visitor.start(beanType);
			if (matching.isEmpty()) {
				this.throwUnableToFindSetter(bean, propertyName);
			}
			if (matching.size() != 1) {
				this.throwTooManySettersFound(bean, propertyName);
			}

			final Method setter = (Method) matching.get(0);
			body.addProperty(setter, value);
			
			context.debug("Inserted statement to set property [" + propertyName + "] upon bean " + bean);

		}
	}

	/**
	 * This method throws an exception when more than one constructor satisfies the specified values for a bean type
	 * @param bean
	 */
	protected void throwTooManySettersFound(final Bean bean, final String propertyName) {
		throw new BeanFactoryGeneratorException("The bean with an id of [" + bean.getId()
				+ "] contains more than one setter for the property [" + propertyName + "]");
	}

	protected void throwUnableToFindSetter(final Bean bean, final String propertyName) {
		throw new BeanFactoryGeneratorException("Unable to find a setter for the property [" + propertyName
				+ "] for the bean with an id of [" + bean.getId() + "].");
	}

	protected ListValue asListValue(final ListTag tag) {
		ObjectHelper.checkNotNull("parameter:tag", tag);

		final ListValue list = new ListValue();
		list.setGeneratorContext(this.getGeneratorContext());

		final Iterator values = tag.getValues().iterator();
		while (values.hasNext()) {
			final ValueTag valueTag = (ValueTag) values.next();
			final Value value = this.asValue(valueTag);
			list.addElement(value);
		}
		return list;
	}

	protected SetValue asSetValue(final SetTag tag) {
		ObjectHelper.checkNotNull("parameter:tag", tag);

		final SetValue set = new SetValue();
		set.setGeneratorContext(this.getGeneratorContext());

		final Iterator values = tag.getValues().iterator();
		while (values.hasNext()) {
			final ValueTag valueTag = (ValueTag) values.next();
			final Value value = this.asValue(valueTag);
			set.addElement(value);
		}
		return set;
	}

	protected MapValue asMapValue(final MapTag tag) {
		ObjectHelper.checkNotNull("parameter:tag", tag);

		final MapValue map = new MapValue();
		map.setGeneratorContext(this.getGeneratorContext());

		final Iterator entries = tag.getValues().entrySet().iterator();
		while (entries.hasNext()) {
			final Map.Entry entry = (Map.Entry) entries.next();
			final String key = (String) entry.getKey();
			final ValueTag valueTag = (ValueTag) entry.getValue();

			final Value value = this.asValue(valueTag);

			map.addEntry(key, value);
		}
		return map;
	}

	/**
	 * Converts a BeanReferenceTag into a BeanReferenceValue
	 * @param tag
	 * @return
	 */
	protected BeanReference asBeanReference(final BeanReferenceTag tag) {
		ObjectHelper.checkNotNull("parameter:tag", tag);

		final String id = tag.getId();
		final Type type = this.getBean(id).getType();

		final BeanReference beanReference = new BeanReference();
		beanReference.setId(id);
		beanReference.setType(type);
		beanReference.setGeneratorContext(this.getGeneratorContext());
		return beanReference;
	}

	protected StringValue asStringValue(final StringTag tag) {
		ObjectHelper.checkNotNull("parameter:tag", tag);

		final GeneratorContext context = this.getGeneratorContext();
		final StringValue string = new StringValue();
		string.setGeneratorContext(context);
		string.setValue(tag.getLiteral());
		string.setType(context.getString());

		return string;
	}

	/**
	 * Converts the given tag into a Value.
	 * @param valueTag
	 * @return
	 */
	protected Value asValue(final ValueTag valueTag) {
		ObjectHelper.checkNotNull("parameter:valueTag", valueTag);

		Value value = null;

		while (true) {
			if (valueTag instanceof ListTag) {
				value = this.asListValue((ListTag) valueTag);
				break;
			}
			if (valueTag instanceof SetTag) {
				value = this.asSetValue((SetTag) valueTag);
				break;
			}
			if (valueTag instanceof MapTag) {
				value = this.asMapValue((MapTag) valueTag);
				break;
			}
			if (valueTag instanceof BeanReferenceTag) {
				value = this.asBeanReference((BeanReferenceTag) valueTag);
				break;
			}

			value = this.asStringValue((StringTag) valueTag);
			break;
		}

		return value;
	}

	/**
	 * Builds all that factory beans that will return a remote json service client.
	 * @param jsonTags A read only list containing all the json tags in the document
	 */
	protected void buildRemoteJsonServiceFactoryBeans(final List jsonTags) {
		ObjectHelper.checkNotNull("parameter:jsonTags", jsonTags);

		this.getGeneratorContext().info("Creating FactoryBean's for each of the remote json service (" + jsonTags.size() + ") tags.");

		final Iterator iterator = jsonTags.iterator();
		while (iterator.hasNext()) {
			final RemoteJsonServiceTag json = (RemoteJsonServiceTag) iterator.next();
			this.createRemoteJsonServiceFactoryBean(json);
		}
	}

	/**
	 * Factory method which creates the initial json FactoryBean
	 * @param jsonTag
	 */
	protected void createRemoteJsonServiceFactoryBean(final RemoteJsonServiceTag jsonTag) {
		ObjectHelper.checkNotNull("parameter:jsonTag", jsonTag);

		final Bean bean = new Bean();
		final String id = jsonTag.getId();
		bean.setId(id);

		final String interfaceName = jsonTag.getInterface();
		final Type beanType = this.getInterfaceType(id, interfaceName);
		bean.setType(beanType);

		final Type superType = this.getPrototypeFactoryBean();
		final NewNestedType factoryBean = this.getBeanFactory().newNestedType();
		factoryBean.setStatic(false);
		factoryBean.setName(id + Constants.FACTORY_BEAN_SUFFIX);
		factoryBean.setSuperType(superType);
		factoryBean.setVisibility(Visibility.PRIVATE);
		bean.setFactoryBean(factoryBean);

		this.getGeneratorContext().debug("Subclassing " + superType + " as the FactoryBean for remote json service: " + bean);

		this.overrideFactoryBeanCreateInstanceViaDeferredBinding(bean);
		this.overrideFactoryBeanSatisfyPropertiesWithSetAddress( bean, jsonTag.getAddress() );
		
		this.addBean(id, bean);
	}
	/**
	 * Builds all the factory beans that will return remote rpc service clients.
	 * @param rpcTags A read only list containing all the rpc tags in the document
	 */
	protected void buildRemoteRpcServiceFactoryBeans(final List rpcTags) {
		ObjectHelper.checkNotNull("parameter:rpcTags", rpcTags);

		this.getGeneratorContext().info("Creating FactoryBean's for each of the remote rpc service (" + rpcTags.size() + ") tags.");

		final Iterator iterator = rpcTags.iterator();
		while (iterator.hasNext()) {
			final RemoteRpcServiceTag rpc = (RemoteRpcServiceTag) iterator.next();
			this.createRemoteRpcServiceFactoryBean(rpc);
		}
	}

	/**
	 * Factory method which creates the initial rpc FactoryBean
	 * @param rpcTag
	 */
	protected void createRemoteRpcServiceFactoryBean(final RemoteRpcServiceTag rpcTag) {
		ObjectHelper.checkNotNull("parameter:rpcTag", rpcTag);

		final Bean bean = new Bean();
		final String id = rpcTag.getId();
		bean.setId(id);

		final String interfaceName = rpcTag.getInterface();
		final Type beanType = this.getInterfaceType(id, interfaceName);
		bean.setType(beanType);

		final Type superType = this.getPrototypeFactoryBean();
		final NewNestedType factoryBean = this.getBeanFactory().newNestedType();
		factoryBean.setStatic(false);
		factoryBean.setName(id + Constants.FACTORY_BEAN_SUFFIX);
		factoryBean.setSuperType(superType);
		factoryBean.setVisibility(Visibility.PRIVATE);
		bean.setFactoryBean(factoryBean);

		this.getGeneratorContext().debug("Subclassing " + superType + " as the FactoryBean for remote rpc service: " + bean);

		this.overrideFactoryBeanCreateInstanceViaDeferredBinding(bean);
		this.overrideFactoryBeanSatisfyPropertiesWithSetAddress( bean, rpcTag.getAddress() );
		
		this.addBean(id, bean);
	}
	
	/**
	 * Overrides the create instance method of the factory bean to create a new instance via deferred binding.
	 * @param bean The bean definition
	 */
	protected void overrideFactoryBeanCreateInstanceViaDeferredBinding( final Bean bean ){
		ObjectHelper.checkNotNull("parameter:bean", bean );
		
		final NewType factoryBean = bean.getFactoryBean();
		final Type beanType = bean.getType();
		
		final NewMethod createInstance = this.createCreateInstanceMethod( factoryBean );		
		final DeferredBinding body = new DeferredBinding();
		body.setType( beanType );		
		createInstance.setBody( body );
		
		this.getGeneratorContext().debug("Inserting statement in FactoryBean.createInstance to create instance via deferred binding, bean: " + bean);
	}
	
	/**
	 * Overrides the satisfyProperties method of the factory bean to set the address property.
	 * @param bean The bean definition
	 * @param address The target of the bean
	 */
	protected void overrideFactoryBeanSatisfyPropertiesWithSetAddress( final Bean bean, final String address ){
		ObjectHelper.checkNotNull("parameter:bean", bean );
		StringHelper.checkNotEmpty("parameter:address", address );
		
		final NewType factoryBean = bean.getFactoryBean();
		
		final Method satisfyProperties = factoryBean.getMostDerivedMethod( Constants.SATISFY_PROPERTIES, this.getParameterListWithOnlyObject() );
		final NewMethod newSatisfyProperties = satisfyProperties.copy( factoryBean );

		final SetPropertiesTemplatedFile body = new SetPropertiesTemplatedFile();
		newSatisfyProperties.setBody( body );
		
		final MethodParameter instance = (MethodParameter)newSatisfyProperties.getParameters().get( 0 );
		body.setInstance(instance);
		final Type serviceDefTarget = this.getServiceDefTarget();
		body.setBean( serviceDefTarget );
		
		final GeneratorContext context = this.getGeneratorContext();
		
		final Type string = context.getString();
		final Method setter = serviceDefTarget.getMostDerivedMethod( Constants.SET_SERVICE_ENTRY_POINT, Arrays.asList( new Type[]{ string }));
		final StringValue addressValue = new StringValue();
		addressValue.setGeneratorContext( context );
		addressValue.setType( string );
		addressValue.setValue( address );
		body.addProperty(setter, addressValue );
		
		context.debug( "Inserting statement to set address property upon bean: " + bean );
	}
	
	/**
	 * Adds a new method to the bean factory being built that populates a map with all the factory beans keyed
	 * by bean id.
	 */
	protected void overrideBeanFactoryBuildFactoryBeans() {
		final Map beans = this.getBeans();

		this.getGeneratorContext().info("Overriding BeanFactory.buildFactoryBeans() to register " + beans.size() + " bean(s).");

		final NewType beanFactory = this.getBeanFactory();
		final Method abstractMethod = beanFactory.getSuperType().getMostDerivedMethod( Constants.BUILD_FACTORY_BEANS, Collections.EMPTY_LIST);
		final NewMethod method = abstractMethod.copy(beanFactory);

		final BuildFactoryBeansTemplatedFile body = new BuildFactoryBeansTemplatedFile();
		method.setBody(body);

		final Iterator beansIterator = beans.values().iterator();
		while (beansIterator.hasNext()) {
			final Bean bean = (Bean) beansIterator.next();
			final String beanId = bean.getId();
			final NewNestedType factoryBean = (NewNestedType) bean.getFactoryBean();

			body.addBean(beanId, factoryBean);
		}
	}

	protected GeneratorContext createGeneratorContext() {
		return new GeneratorContext() {
			protected String getGeneratedTypeNameSuffix() {
				return Constants.BEAN_FACTORY_SUFFIX;
			}
		};
	}

	/**
	 * The beanFactoryImpl being assembled
	 */
	private NewConcreteType beanFactory;

	public NewConcreteType getBeanFactory() {
		ObjectHelper.checkNotNull("field:beanFactory", beanFactory);
		return this.beanFactory;
	}

	protected void setBeanFactory(final NewConcreteType beanFactory) {
		ObjectHelper.checkNotNull("parameter:beanFactory", beanFactory);
		this.beanFactory = beanFactory;
	}

	/**
	 * A map that contains all the NewType factory beans for each defined bean keyed on the bean's id.
	 */
	private Map beans;

	public Map getBeans() {
		ObjectHelper.checkNotNull("field:beans", beans);
		return this.beans;
	}

	protected void setBeans(final Map beans) {
		ObjectHelper.checkNotNull("parameter:beans", beans);
		this.beans = beans;
	}

	protected Map createBeans() {
		return new HashMap();
	}

	/**
	 * Adds a new bean to the beans registry (a map).
	 * @param id
	 * @param factoryBean
	 */
	protected void addBean(final String id, final Bean bean) {
		StringHelper.checkNotEmpty("parameter:id", id);
		ObjectHelper.checkNotNull("parameter:bean", bean);

		this.getBeans().put(id, bean);
	}

	/**
	 * Retrieves a bean definition given an id. If the bean is not found an exception is thrown. 
	 * @param id
	 * @return
	 */
	protected Bean getBean(final String id) {
		final Bean bean = (Bean) this.getBeans().get(id);
		ObjectHelper.checkNotNull("bean", bean);
		return bean;
	}
	
	protected Type getSingletonFactoryBean() {
		return this.getGeneratorContext().getType(Constants.SINGLETON_FACTORY_BEAN);
	}

	protected Type getPrototypeFactoryBean() {
		return this.getGeneratorContext().getType(Constants.PROTOTYPE_FACTORY_BEAN);
	}

	protected Type getBeanFactoryType() {
		return this.getGeneratorContext().getType(Constants.BEAN_FACTORY);
	}

	protected Type getBeanFactoryImpl() {
		return this.getGeneratorContext().getType(Constants.BEAN_FACTORY_IMPL);
	}
	
	protected Type getServiceDefTarget() {
		return this.getGeneratorContext().getType(Constants.SERVICE_DEF_TARGET);
	}

	/**
	 * Fetches the interface type throwing an excecption if the type is not found or not an interface.
	 * @param id
	 * @param className
	 * @return
	 */
	protected Type getInterfaceType(final String id, final String className) {
		final Type type = this.getGeneratorContext().findType(className);
		if (null == type) {
			throwBeanTypeNotFound(id, className);
		}
		if ( false == type.isInterface() || type.isPrimitive()) {
			throwBeanTypeIsNotAnInterface(id, type);
		}
		return type;
	}

	protected void throwBeanTypeIsNotAnInterface(final String id, final Type type) {
		throw new BeanFactoryGeneratorException("The type [" + type + "] is not an interface for the bean id[" + id + "]");
	}

	/**
	 * Fetches the concrete type. If the type is not found or is an interface exceptions are thrown.
	 * @param id
	 * @param className
	 * @return
	 */
	protected Type getConcreteType(final String id, final String className) {
		final Type type = this.getGeneratorContext().findType(className);
		if (null == type) {
			throwBeanTypeNotFound(id, className);
		}
		if (type.isInterface() || type.isAbstract() || type.isPrimitive()) {
			throwBeanTypeIsNotConcrete(id, type);
		}
		return type;
	}

	protected List getParameterListWithOnlyObject(){
		return Arrays.asList( new Type[]{ this.getGeneratorContext().getObject() } );
	}
	
	protected void throwBeanTypeNotFound(final String id, final String className) {
		throw new BeanFactoryGeneratorException("Unable to find the type [" + className + "] for the bean with an id of [" + id + "]");
	}

	protected void throwBeanTypeIsNotConcrete(final String id, final Type type) {
		throw new BeanFactoryGeneratorException("The type [" + type + "] is not concrete for the bean id[" + id + "]");
	}
}
