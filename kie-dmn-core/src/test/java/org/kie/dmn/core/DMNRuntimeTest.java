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

package org.kie.dmn.core;

import org.junit.Test;
import org.kie.dmn.core.api.*;
import org.kie.dmn.core.api.event.*;
import org.kie.dmn.core.util.DMNRuntimeUtil;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class DMNRuntimeTest {

    @Test
    public void testCompositeItemDefinition() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "0008-LX-arithmetic.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "https://github.com/droolsjbpm/kie-dmn", "0008-LX-arithmetic" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        Map loan = new HashMap();
        loan.put( "principal", 600000 );
        loan.put( "rate", 0.0375 );
        loan.put( "termMonths", 360 );
        context.set( "loan", loan );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );

        DMNContext result = dmnResult.getContext();

        assertThat( result.get( "payment" ), is( new BigDecimal( "2778.693549432766720839844710324306" ) ) );
    }

    @Test
    public void testTrisotechNamespace() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "trisotech_namespace.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_b8feec86-dadf-4051-9feb-8e6093bbb530", "Solution 3" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "IsDoubleHulled", true );
        context.set( "Residual Cargo Size", new BigDecimal( 0.1 ) );
        context.set( "Ship Size", new BigDecimal( 50 ) );
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "Ship can enter a Dutch port" ), is( true ) );
    }

    @Test
    public void testEmptyDecision1() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "empty_decision.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_ba9fc4b1-5ced-4d00-9b61-290de4bf3213", "Solution 3" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        Map shipInfo = new HashMap();
        shipInfo.put( "Size", BigDecimal.valueOf( 70 ) );
        shipInfo.put( "Is Double Hulled", Boolean.FALSE );
        shipInfo.put( "Residual Cargo Size", BigDecimal.valueOf( 0.1 ) );
        context.set( "Ship Info", shipInfo );

        // Test that even if one decision is empty or missing input data,
        // the other decisions in the model are still evaluated
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        DMNContext result = dmnResult.getContext();
        assertThat( dmnResult.hasErrors(), is( true ) );
        assertThat( result.get( "Ship Can Enter v2" ), is( true ) );
    }

    @Test
    public void testEmptyDecision2() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "empty_decision.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_ba9fc4b1-5ced-4d00-9b61-290de4bf3213", "Solution 3" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        Map shipInfo = new HashMap();
        shipInfo.put( "Size", BigDecimal.valueOf( 70 ) );
        shipInfo.put( "Is Double Hulled", Boolean.FALSE );
        shipInfo.put( "Residual Cargo Size", BigDecimal.valueOf( 0.1 ) );
        context.set( "Ship Info", shipInfo );
        context.set( "Ship Size", BigDecimal.valueOf( 70 ) );
        context.set( "IsDoubleHulled", Boolean.FALSE );
        context.set( "Residual Cargo Size", BigDecimal.valueOf( 0.1 ) );

        // check that if all the input data is available, but the
        // decision expression is empty, the model returns a warning
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );

        List<DMNMessage> messages = dmnResult.getMessages( DMNMessage.Severity.WARN );
        assertThat( messages.size(), is( 1 ) );
        assertThat( messages.get( 0 ).getSeverity(), is( DMNMessage.Severity.WARN ) );
        assertThat( messages.get( 0 ).getSourceId(), is( "_42806504-8ed5-488f-b274-de98c1bc67b9" ) );

        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "Ship Can Enter v2" ), is( true ) );
    }

    @Test
    public void testEventListeners() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "car_damage_responsibility.dmn", this.getClass() );

        DMNRuntimeEventListener listener = mock( DMNRuntimeEventListener.class );
        runtime.addListener( listener );
        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_820611e9-c21c-47cd-8e52-5cba2be9f9cc", "Car Damage Responsibility" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "Membership Level", "Silver" );
        context.set( "Damage Types", "Body" );
        context.set( "Responsible", "Driver" );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );

        ArgumentCaptor<AfterEvaluateDecisionTableEvent> argument = ArgumentCaptor.forClass( AfterEvaluateDecisionTableEvent.class );
        verify( listener, times( 2 ) )
                .beforeEvaluateDecision( any( BeforeEvaluateDecisionEvent.class ) );
        verify( listener, times( 2 ) )
                .afterEvaluateDecision( any( AfterEvaluateDecisionEvent.class ) );
        verify( listener, times( 2 ) )
                .beforeEvaluateDecisionTable( any( BeforeEvaluateDecisionTableEvent.class ) );
        verify( listener, times( 2 ) )
                .afterEvaluateDecisionTable( argument.capture() );

        AfterEvaluateDecisionTableEvent dte = argument.getAllValues().get( 0 );
        assertThat( dte.getDecisionTableName(), is( "Car Damage Responsibility" ) );
        assertThat( dte.getMatches(), is( Arrays.asList( 5 ) ) ); // rows are 1-based

        dte = argument.getAllValues().get( 1 );
        assertThat( dte.getDecisionTableName(), is( "Payment method" ) );
        assertThat( dte.getMatches(), is( Arrays.asList( 3 ) ) ); // rows are 1-based

        assertThat( dmnResult.hasErrors(), is( false ) );

        DMNContext result = dmnResult.getContext();
        assertThat( (Map<String, Object>) result.get( "Car Damage Responsibility" ), hasEntry( is( "EU Rent" ), is( BigDecimal.valueOf( 40 ) ) ) );
        assertThat( (Map<String, Object>) result.get( "Car Damage Responsibility" ), hasEntry( is( "Renter" ), is( BigDecimal.valueOf( 60 ) ) ) );
        assertThat( result.get( "Payment method" ), is( "Check" ) );
    }

    @Test
    public void testOutputReuse() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "Input_reuse_in_output.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_098bb607-eff7-4772-83ac-6ded8b371fa7", "Input reuse in output" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "Age", 40 );
        context.set( "Requested Product", "Fixed30" );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        System.out.println( dmnResult.getMessages() );
        assertThat( dmnResult.hasErrors(), is( false ) );

        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "My Decision" ), is( "Fixed30" ) );
    }

    @Test
    public void testSimpleNot() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "Simple_Not.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_98436ebb-7c42-48c0-8d11-d693e2a817c9", "Simple Not" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "Occupation", "Student" );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        System.out.println( dmnResult.getMessages() );
        assertThat( dmnResult.hasErrors(), is( false ) );

        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "a" ), is( "Is Student" ) );
    }

    @Test
    public void testSimpleNot2() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "Simple_Not.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_98436ebb-7c42-48c0-8d11-d693e2a817c9", "Simple Not" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "Occupation", "Engineer" );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        System.out.println( dmnResult.getMessages() );
        assertThat( dmnResult.hasErrors(), is( false ) );

        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "a" ), is( "Is not a Student" ) );
    }

    @Test
    public void testDinner() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "Dinner.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_0c45df24-0d57-4acc-b296-b4cba8b71a36", "Dinner" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        context.set( "Guests with children", true );
        context.set( "Season", "Fall" );
        context.set( "Number of guests", 4 );
        context.set( "Temp", 25 );
        context.set( "Rain Probability", 30 );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( dmnResult.getContext().get( "Where to eat" ), is( "Outside" ) );
        assertThat( dmnResult.getContext().get( "Dish" ), is( "Spareribs" ) );
        assertThat( dmnResult.getContext().get( "Drinks" ), is( Arrays.asList( "Apero", "Ale", "Juice Boxes" ) ) );
    }

    @Test
    public void testNotificationsApproved2() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "NotificationsTest2.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "https://github.com/droolsjbpm/kie-dmn", "building-structure-rules" );
        assertThat( dmnModel, notNullValue() );

        DMNContext context = DMNFactory.newContext();
        context.set( "existingActivityApplicability", true );
        context.set( "Distance", new BigDecimal( 9999 ) );
        context.set( "willIncreaseTraffic", true );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        DMNContext result = dmnResult.getContext();
        assertThat( result.get( "Notification Status" ), is( "Notification to Province Approved" ) );
        assertThat( result.get( "Permit Status" ), is( "Building Activity Province Permit Required" ) );
    }

    @Test
    public void testBoxedContext() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "BoxedContext.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_0de36357-fec0-4b4e-b7f1-382d381e06e9", "Dessin 1" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        context.set( "a", 10 );
        context.set( "b", 5 );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( (Map<String, Object>) dmnResult.getContext().get( "Math" ), hasEntry( "Sum", BigDecimal.valueOf( 15 ) ) );
        assertThat( (Map<String, Object>) dmnResult.getContext().get( "Math" ), hasEntry( "Product", BigDecimal.valueOf( 50 ) ) );
    }

    @Test
    public void testFunctionDefAndInvocation() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "FunctionDefinition.dmn", this.getClass() );
        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/dmn/definitions/_0de36357-fec0-4b4e-b7f1-382d381e06e9", "Dessin 1" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        context.set( "a", 10 );
        context.set( "b", 5 );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( (Map<String, Object>) dmnResult.getContext().get( "Math" ), hasEntry( "Sum", BigDecimal.valueOf( 15 ) ) );
    }

    @Test
    public void testBKMNode() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "0009-invocation-arithmetic.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_cb28c255-91cd-4c01-ac7b-1a9cb1ecdb11", "literal invocation1" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        Map<String, Object> loan = new HashMap<>();
        loan.put( "amount", BigDecimal.valueOf( 600000 ) );
        loan.put( "rate", new BigDecimal( "0.0375" ) );
        loan.put( "term", BigDecimal.valueOf( 360 ) );
        DMNContext context = DMNFactory.newContext();
        context.set( "fee", 100 );
        context.set( "Loan", loan );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat(
                ((BigDecimal) dmnResult.getContext().get( "MonthlyPayment" )).setScale( 8, BigDecimal.ROUND_DOWN ),
                is( new BigDecimal( "2878.69354943277" ).setScale( 8, BigDecimal.ROUND_DOWN ) ) );
    }

    @Test
    public void testItemDefCollection() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "0001-filter.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_f52ca843-504b-4c3b-a6bc-4d377bffef7a", "filter01" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        Object[][] data = new Object[][]{
                {1, "Finances", "John"},
                {2, "Engineering", "Mary"},
                {3, "Sales", "Kevin"}
        };
        List<Map<String, Object>> employees = new ArrayList<>();
        for ( int i = 0; i < data.length; i++ ) {
            Map<String, Object> e = new HashMap<>();
            e.put( "id", data[i][0] );
            e.put( "dept", data[i][1] );
            e.put( "name", data[i][2] );
            employees.add( e );
        }
        DMNContext context = DMNFactory.newContext();
        context.set( "Employee", employees );

        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( dmnResult.getContext().get( "filter01" ), is( Arrays.asList( "Mary" ) ) );
    }

    @Test
    public void testList() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "list-expression.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "https://github.com/droolsjbpm/kie-dmn", "list-expression" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( dmnResult.getContext().get( "Name list" ), is( Arrays.asList( "John", "Mary" ) ) );
    }

    @Test
    public void testRelation() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "relation-expression.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "https://github.com/droolsjbpm/kie-dmn", "relation-expression" );
        assertThat( dmnModel, notNullValue() );
        assertThat( dmnModel.getMessages().toString(), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        assertThat( dmnResult.hasErrors(), is( false ) );
        assertThat( dmnResult.getContext().get( "Employee Relation" ), is( instanceOf( List.class ) ) );

        List<Map<String, Object>> employees = (List<Map<String, Object>>) dmnResult.getContext().get( "Employee Relation" );
        Map<String, Object> e = employees.get( 0 );
        assertThat( e.get( "Name" ), is( "John" ) );
        assertThat( e.get( "Dept" ), is( "Sales" ) );
        assertThat( e.get( "Salary" ), is( BigDecimal.valueOf( 100000 ) ) );

        e = employees.get( 1 );
        assertThat( e.get( "Name" ), is( "Mary" ) );
        assertThat( e.get( "Dept" ), is( "Finances" ) );
        assertThat( e.get( "Salary" ), is( BigDecimal.valueOf( 120000 ) ) );
    }

    @Test
    public void testMissingInputData() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "missing_input_data.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_4047acf3-fce2-42f3-abf2-fb06282c1ea0", "Upgrade Based On Promotions" );
        assertThat( dmnModel, notNullValue() );
        assertThat( formatMessages( dmnModel.getMessages() ), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        context.set( "Requested Vehicle Class", "Compact" );
        context.set( "Membership Level", "Silver" );
        context.set( "Calendar Promotion", "None" );
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        //        System.out.println(formatMessages( dmnResult.getMessages() ));
        // work in progress... later we will check the actual messages...
        //        assertThat( formatMessages( dmnResult.getMessages() ), dmnResult.getMessages( DMNMessage.Severity.ERROR ).size(), is( 4 ) );
    }

    @Test
    public void testLendingExample() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "0004-lending.dmn", getClass() );
//        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_4e0f0b70-d31c-471c-bd52-5ca709ed362b", "Lending1" );
        assertThat( dmnModel, notNullValue() );
        assertThat( formatMessages( dmnModel.getMessages() ), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        Map applicant = new HashMap();
        Map monthly = new HashMap();
        monthly.put( "Income", 6000 );
        monthly.put( "Expenses", 2000 );
        monthly.put( "Repayments", 0 );
        applicant.put( "Monthly", monthly );
        applicant.put( "Age", 35 );
        applicant.put( "ExistingCustomer", true );
        applicant.put( "MaritalStatus", "M" );
        applicant.put( "EmploymentStatus", "EMPLOYED" );
        Map product = new HashMap();
        product.put( "ProductType", "STANDARD LOAN" );
        product.put( "Amount", 350000 );
        product.put( "Rate", new BigDecimal( "0.0395" ) );
        product.put( "Term", 360 );
        Map bureau = new HashMap();
        bureau.put( "CreditScore", 649 );
        bureau.put( "Bankrupt", false );

        context.set( "ApplicantData", applicant );
        context.set( "RequestedProduct", product );
        context.set( "BureauData", bureau );
        context.set( "SupportingDocuments", "yes" );
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        System.out.println( formatMessages( dmnResult.getMessages() ) );
        DMNContext ctx = dmnResult.getContext();
        System.out.println( ctx );

        assertThat( ctx.get( "ApplicationRiskScore" ), is( BigDecimal.valueOf( 130 ) ) );
        assertThat( ctx.get( "Pre-bureauRiskCategory" ), is( "LOW" ) );
        assertThat( ctx.get( "BureauCallType" ), is( "MINI" ) );
        assertThat( ctx.get( "Post-bureauRiskCategory" ), is( "LOW" ) );
        assertThat( ((BigDecimal)ctx.get( "RequiredMonthlyInstallment" )).setScale( 5, BigDecimal.ROUND_DOWN ),
                    is( new BigDecimal( "1680.880325608555" ).setScale( 5, BigDecimal.ROUND_DOWN ) ) );
        assertThat( ctx.get( "Pre-bureauAffordability" ), is( true ) );
        assertThat( ctx.get( "Eligibility" ), is( "ELIGIBLE" ) );
        assertThat( ctx.get( "Strategy" ), is( "BUREAU" ) );
        assertThat( ctx.get( "Post-bureauAffordability" ), is( true ) );
        assertThat( ctx.get( "Routing" ), is( "ACCEPT" ) );
    }

    @Test
    public void testDateAndTime() {
        DMNRuntime runtime = DMNRuntimeUtil.createRuntime( "0007-date-time.dmn", getClass() );
        runtime.addListener( DMNRuntimeUtil.createListener() );

        DMNModel dmnModel = runtime.getModel( "http://www.trisotech.com/definitions/_69430b3e-17b8-430d-b760-c505bf6469f9", "dateTime Table 58" );
        assertThat( dmnModel, notNullValue() );
        assertThat( formatMessages( dmnModel.getMessages() ), dmnModel.hasErrors(), is( false ) );

        DMNContext context = DMNFactory.newContext();
        context.set( "dateString", "2015-12-24" );
        context.set( "timeString", "00:00:01-01:00" );
        context.set( "dateTimeString", "2016-12-24T23:59:00-05:00" );
        context.set( "Hours", 12 );
        context.set( "Minutes", 59 );
        context.set( "Seconds", new BigDecimal( "1.3" ) );
        context.set( "Timezone", "PT-1H" );
        context.set( "Year", 1999 );
        context.set( "Month", 11 );
        context.set( "Day", 22 );
        context.set( "oneHour", "PT1H" );
        context.set( "durationString", "P13DT2H14S" );
        DMNResult dmnResult = runtime.evaluateAll( dmnModel, context );
        System.out.println( formatMessages( dmnResult.getMessages() ) );
        DMNContext ctx = dmnResult.getContext();
        System.out.println( ctx );

        assertThat( ctx.get("Date-Time"), is( ZonedDateTime.of( 2016, 12, 24, 23, 59, 0, 0, ZoneOffset.ofHours( -5 ) ) ) );
        assertThat( ctx.get("Date"), is( new HashMap<String, Object>(  ) {{
            put( "fromString", LocalDate.of( 2015, 12, 24 ) );
            put( "fromDateTime", LocalDate.of( 2016, 12, 24 ) );
            put( "fromYearMonthDay", LocalDate.of( 1999, 11, 22 ) );
        }} ) );
        assertThat( ctx.get("Time"), is( OffsetTime.of( 00, 00, 01, 00, ZoneOffset.ofHours( -1 ) ) ) );
        assertThat( ctx.get("Date-Time2"), is( ZonedDateTime.of( 2015, 12, 24, 00, 00, 01, 00, ZoneOffset.ofHours( -1 ) ) ) );
        assertThat( ctx.get("Time2"), is( OffsetTime.of( 00, 00, 01, 00, ZoneOffset.ofHours( -1 ) ) ) );
        assertThat( ctx.get("Time3"), is( OffsetTime.of( 12, 59, 1, 300000000, ZoneOffset.ofHours( -1 ) )) );
        assertThat( ctx.get("dtDuration1"), is( Duration.parse( "P13DT2H14S" ) ) );
        assertThat( ctx.get("dtDuration2"), is( Duration.parse( "P367DT3H58M59S" ) ) );
        assertThat( ctx.get("hoursInDuration"), is( new BigDecimal( "3" ) ) );
        assertThat( ctx.get("sumDurations"), is( Duration.parse( "PT9125H59M13S" ) ) );
        assertThat( ctx.get("ymDuration2"), is( Period.parse( "P1Y" ) ) );
        assertThat( ctx.get("cDay"), is( BigDecimal.valueOf( 24 ) ) );
        assertThat( ctx.get("cYear"), is( BigDecimal.valueOf( 2015 ) ) );
        assertThat( ctx.get("cMonth"), is( BigDecimal.valueOf( 12 ) ) );
        assertThat( ctx.get("cHour"), is( BigDecimal.valueOf( 0 ) ) );
        assertThat( ctx.get("cMinute"), is( BigDecimal.valueOf( 0 ) ) );
        assertThat( ctx.get("cSecond"), is( BigDecimal.valueOf( 1 ) ) );
        assertThat( ctx.get("cTimezone"), is( Duration.parse( "PT-1H" ) ) );
        assertThat( ctx.get("years"), is( BigDecimal.valueOf( 1 ) ) );
        assertThat( ctx.get("seconds"), is( BigDecimal.valueOf( 14 ) ) );

    }
    private String formatMessages(List<DMNMessage> messages) {
        return messages.stream().map( m -> m.toString() ).collect( Collectors.joining( "\n" ) );
    }


}

