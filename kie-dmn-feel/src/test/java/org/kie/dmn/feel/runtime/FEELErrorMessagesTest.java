/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.feel.runtime;

import org.junit.Test;
import org.kie.dmn.feel.FEEL;
import org.kie.dmn.feel.lang.CompiledExpression;
import org.kie.dmn.feel.lang.CompilerContext;
import org.kie.dmn.feel.runtime.events.FEELEvent;
import org.kie.dmn.feel.runtime.events.FEELEventListener;

public class FEELErrorMessagesTest {

    @Test
    public void unknownVariable() {
        FEEL feel = FEEL.newInstance();
        feel.addListener( new FEELEventListener() {
            @Override
            public void onEvent(FEELEvent event) {
                System.out.println(event);
            }
        } );

        CompilerContext ctx = feel.newCompilerContext();
//        ctx.addInputVariableType( "a variable name", BuiltInType.UNKNOWN );
        CompiledExpression ce = feel.compile( "a variable name", ctx );
        System.out.println(ce);

    }
}