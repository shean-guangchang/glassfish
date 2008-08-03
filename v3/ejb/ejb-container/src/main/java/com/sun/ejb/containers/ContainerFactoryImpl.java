/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.ejb.containers;

import com.sun.ejb.Container;
import com.sun.ejb.ContainerFactory;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbMessageBeanDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.deployment.runtime.IASEjbExtraDescriptors;
import com.sun.enterprise.security.SecurityContext;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.ejb.security.application.EJBSecurityManager;
import org.glassfish.ejb.deployment.EjbSingletonDescriptor;

import java.util.ArrayList;

public final class ContainerFactoryImpl implements ContainerFactory {

    public Container createContainer(EjbDescriptor ejbDescriptor,
				     ClassLoader loader, 
				     EJBSecurityManager sm,
				     DeploymentContext dynamicConfigContext)
	     throws Exception 
    {
        BaseContainer container = null;
        boolean hasHome = true;
        String appid = ejbDescriptor.getApplication().getRegistrationName();
        String archiveuri = ejbDescriptor.getEjbBundleDescriptor().
            getModuleDescriptor().getArchiveUri();
            
        String modulename = "";
            //TODO com.sun.enterprise.util.io.FileUtils.makeFriendlyFilename(archiveuri);
        String ejbname = ejbDescriptor.getName();

        IASEjbExtraDescriptors iased = null;

        try {
            // instantiate container class
            if (ejbDescriptor instanceof EjbSingletonDescriptor) {
                EjbSingletonDescriptor sd = (EjbSingletonDescriptor) ejbDescriptor;
                
                System.out.println("** [SingletonContainer] Got EJB Descriptor: " + sd.isSingleton());
                if (sd.isSingleton()) {
                    System.out.println("** [SingletonContainer] isStateless: " + sd.isStateless() + " **");
                    container = new SingletonContainer(ejbDescriptor, loader);
                }
            } else if (ejbDescriptor instanceof EjbSessionDescriptor) {
                EjbSessionDescriptor sd = (EjbSessionDescriptor)ejbDescriptor;
                if ( sd.isStateless() ) {
                    if ((ejbDescriptor.getLocalClassName() != null) &&
                            (ejbDescriptor.getLocalBusinessClassNames()
                             .contains("com.sun.ejb.containers.TimerLocal"))) {
                        container = new TimerBeanContainer(ejbDescriptor, loader);
                    } else {
                        container = new StatelessSessionContainer(ejbDescriptor, loader);
                    }
                } else {
                    /*TODO
                    //container = new StatefulSessionContainer(ejbDescriptor, loader);
		    BaseContainerBuilder builder =
			new StatefulContainerBuilder();
		    builder.buildContainer(ejbDescriptor, loader,
			dynamicConfigContext);
		    container = builder.getContainer();
		    //containers.put(ejbDescriptor.getUniqueId(), container);
		    //builder.completeInitialization(sm);
                */
                }
            } else if ( ejbDescriptor instanceof EjbMessageBeanDescriptor) {
                //TODO container = new MessageBeanContainer(ejbDescriptor, loader);
		// Message-driven beans don't have a home or remote interface.
                hasHome = false;
            }
		
            //container.setSecurityManager(sm);

            if ( hasHome ) {
                container.initializeHome();
            }

            return container;
        } catch ( Exception ex ) {
            throw ex;
        }
    }

} //ContainerFactoryImpl


class BeanContext {
    ClassLoader previousClassLoader;
    boolean classLoaderSwitched;
    SecurityContext
            previousSecurityContext;
}

class ArrayListStack
    extends ArrayList
{
    /**
     * Creates a stack with the given initial size
     */
    public ArrayListStack(int size) {
        super(size);
    }
    
    /**
     * Creates a stack with a default size
     */
    public ArrayListStack() {
        super();
    }

    /**
     * Pushes an item onto the top of this stack. This method will internally
     * add elements to the <tt>ArrayList</tt> if the stack is full.
     *
     * @param   obj   the object to be pushed onto this stack.
     * @see     java.util.ArrayList#add
     */
    public void push(Object obj) {
        super.add(obj);
    }

    /**
     * Removes the object at the top of this stack and returns that 
     * object as the value of this function. 
     *
     * @return     The object at the top of this stack (the last item 
     *             of the <tt>ArrayList</tt> object). Null if stack is empty.
     */
    public Object pop() {
        int sz = super.size();
        return (sz > 0) ? super.remove(sz-1) : null;
    }
    
    /**
     * Tests if this stack is empty.
     *
     * @return  <code>true</code> if and only if this stack contains 
     *          no items; <code>false</code> otherwise.
     */
    public boolean empty() {
        return super.size() == 0;
    }

    /**
     * Looks at the object at the top of this stack without removing it 
     * from the stack. 
     *
     * @return     the object at the top of this stack (the last item 
     *             of the <tt>ArrayList</tt> object).  Null if stack is empty.
     */
    public Object peek() {
        int sz = size();
        return (sz > 0) ? super.get(sz-1) : null;
    }



} //ArrayListStack

