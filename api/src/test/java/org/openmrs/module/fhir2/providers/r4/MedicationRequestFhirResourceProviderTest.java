/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2.providers.r4;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.ReferenceAndListParam;
import ca.uhn.fhir.rest.param.ReferenceOrListParam;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenAndListParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirMedicationRequestService;

@RunWith(MockitoJUnitRunner.class)
public class MedicationRequestFhirResourceProviderTest {
	
	private static final String MEDICATION_REQUEST_UUID = "c0938432-1691-11df-97a5-7038c432aaba";
	
	private static final String WRONG_MEDICATION_REQUEST_UUID = "c0938432-1691-11df-97a5-7038c432aaba";
	
	private static final String LAST_UPDATED_DATE = "2020-09-03";
	
	@Mock
	private FhirMedicationRequestService fhirMedicationRequestService;
	
	private MedicationRequestFhirResourceProvider resourceProvider;
	
	private MedicationRequest medicationRequest;
	
	@Before
	public void setup() {
		resourceProvider = new MedicationRequestFhirResourceProvider();
		resourceProvider.setFhirMedicationRequestService(fhirMedicationRequestService);
		
		medicationRequest = new MedicationRequest();
		medicationRequest.setId(MEDICATION_REQUEST_UUID);
	}
	
	@Test
	public void getResourceType_shouldReturnResourceType() {
		assertThat(resourceProvider.getResourceType(), equalTo(MedicationRequest.class));
		assertThat(resourceProvider.getResourceType().getName(), equalTo(MedicationRequest.class.getName()));
	}
	
	@Test
	public void getMedicationRequestByUuid_shouldReturnMatchingMedicationRequest() {
		when(fhirMedicationRequestService.get(MEDICATION_REQUEST_UUID)).thenReturn(medicationRequest);
		
		IdType id = new IdType();
		id.setValue(MEDICATION_REQUEST_UUID);
		MedicationRequest medicationRequest = resourceProvider.getMedicationRequestByUuid(id);
		assertThat(medicationRequest, notNullValue());
		assertThat(medicationRequest.getId(), notNullValue());
		assertThat(medicationRequest.getId(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void getMedicationRequestByUuid_shouldThrowResourceNotFoundException() {
		IdType id = new IdType();
		id.setValue(WRONG_MEDICATION_REQUEST_UUID);
		MedicationRequest medicationRequest = resourceProvider.getMedicationRequestByUuid(id);
		assertThat(medicationRequest, nullValue());
	}
	
	private List<IBaseResource> getResources(IBundleProvider results, int theFromIndex, int theToIndex) {
		return results.getResources(theFromIndex, theToIndex);
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestUsingCode() {
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		TokenAndListParam code = new TokenAndListParam();
		TokenParam codingToken = new TokenParam();
		codingToken.setValue("1000");
		code.addAnd(codingToken);
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, null, code, null, null, null,
		    null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenPatientParamIsSpecified() {
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		ReferenceAndListParam patientParam = new ReferenceAndListParam();
		patientParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Patient.SP_NAME)));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(patientParam, null, null, null, null, null,
		    null, null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenMedicationParamIsSpecified() {
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		ReferenceAndListParam medicationParam = new ReferenceAndListParam();
		medicationParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Medication.SP_IDENTIFIER)));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, null, null, null, medicationParam,
		    null, null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenParticipantParamIsSpecified() {
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		ReferenceAndListParam participantParam = new ReferenceAndListParam();
		participantParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Practitioner.SP_NAME)));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, null, null, participantParam,
		    null, null, null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenEncounterParamIsSpecified() {
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		ReferenceAndListParam encounterParam = new ReferenceAndListParam();
		encounterParam.addValue(new ReferenceOrListParam().add(new ReferenceParam().setChain(Encounter.SP_IDENTIFIER)));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, encounterParam, null, null, null,
		    null, null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenUUIDIsSpecified() {
		TokenAndListParam uuid = new TokenAndListParam().addAnd(new TokenParam(MEDICATION_REQUEST_UUID));
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, null, null, null, null, uuid,
		    null);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void searchMedicationRequest_shouldReturnMatchingMedicationRequestWhenLastUpdatedIsSpecified() {
		DateRangeParam lastUpdated = new DateRangeParam().setUpperBound(LAST_UPDATED_DATE).setLowerBound(LAST_UPDATED_DATE);
		
		when(fhirMedicationRequestService.searchForMedicationRequests(any(), any(), any(), any(), any(), any(), any()))
		        .thenReturn(new MockIBundleProvider<>(Collections.singletonList(medicationRequest), 10, 1));
		
		IBundleProvider results = resourceProvider.searchForMedicationRequests(null, null, null, null, null, null, null,
		    lastUpdated);
		
		List<IBaseResource> resources = getResources(results, 1, 5);
		
		assertThat(results, notNullValue());
		assertThat(resources, hasSize(equalTo(1)));
		assertThat(resources.get(0), notNullValue());
		assertThat(resources.get(0).fhirType(), equalTo(FhirConstants.MEDICATION_REQUEST));
		assertThat(resources.get(0).getIdElement().getIdPart(), equalTo(MEDICATION_REQUEST_UUID));
	}
	
	@Test
	public void createMedicationRequest_shouldCreateMedicationRequest() {
		when(fhirMedicationRequestService.create(any(MedicationRequest.class))).thenReturn(medicationRequest);
		
		MethodOutcome result = resourceProvider.createMedicationRequest(medicationRequest);
		assertThat(result, notNullValue());
		assertThat(result.getCreated(), is(true));
		assertThat(result.getResource().getIdElement().getIdPart(), equalTo(medicationRequest.getId()));
		
		verify(fhirMedicationRequestService, atLeastOnce()).create(medicationRequest);
	}
	
	@Test
	public void updateMedicationRequest_shouldUpdateMedicationRequest() {
		medicationRequest.setStatus(MedicationRequest.MedicationRequestStatus.STOPPED);
		
		when(fhirMedicationRequestService.update(eq(MEDICATION_REQUEST_UUID), any(MedicationRequest.class)))
		        .thenReturn(medicationRequest);
		
		MethodOutcome result = resourceProvider.updateMedicationRequest(new IdType().setValue(MEDICATION_REQUEST_UUID),
		    medicationRequest);
		
		assertThat(result, notNullValue());
		assertThat(result.getResource(), notNullValue());
		assertThat(result.getResource().getIdElement().getIdPart(), equalTo(medicationRequest.getId()));
		
		verify(fhirMedicationRequestService, atLeastOnce()).update(eq(MEDICATION_REQUEST_UUID),
		    any(MedicationRequest.class));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void updateMedicationRequest_shouldThrowInvalidRequestForUuidMismatch() {
		when(fhirMedicationRequestService.update(eq(WRONG_MEDICATION_REQUEST_UUID), any(MedicationRequest.class)))
		        .thenThrow(InvalidRequestException.class);
		
		resourceProvider.updateMedicationRequest(new IdType().setValue(WRONG_MEDICATION_REQUEST_UUID), medicationRequest);
		
		verify(fhirMedicationRequestService, atLeastOnce()).update(eq(WRONG_MEDICATION_REQUEST_UUID),
		    any(MedicationRequest.class));
	}
	
	@Test(expected = MethodNotAllowedException.class)
	public void updateMedicationShouldThrowMethodNotAllowedIfDoesNotExist() {
		MedicationRequest wrongMedicationRequest = new MedicationRequest();
		wrongMedicationRequest.setId(WRONG_MEDICATION_REQUEST_UUID);
		
		when(fhirMedicationRequestService.update(eq(WRONG_MEDICATION_REQUEST_UUID), any(MedicationRequest.class)))
		        .thenThrow(MethodNotAllowedException.class);
		
		resourceProvider.updateMedicationRequest(new IdType().setValue(WRONG_MEDICATION_REQUEST_UUID),
		    wrongMedicationRequest);
		
		verify(fhirMedicationRequestService, atLeastOnce()).update(eq(WRONG_MEDICATION_REQUEST_UUID),
		    any(MedicationRequest.class));
	}
	
	@Test
	public void deleteTask_shouldDeleteMedicationRequest() {
		when(fhirMedicationRequestService.delete(MEDICATION_REQUEST_UUID)).thenReturn(medicationRequest);
		OperationOutcome result = resourceProvider.deleteMedicationRequest(new IdType().setValue(MEDICATION_REQUEST_UUID));
		assertThat(result, notNullValue());
		assertThat(result.getIssue(), notNullValue());
		assertThat(result.getIssueFirstRep().getSeverity(), equalTo(OperationOutcome.IssueSeverity.INFORMATION));
		assertThat(result.getIssueFirstRep().getDetails().getCodingFirstRep().getCode(), equalTo("MSG_DELETED"));
		
		verify(fhirMedicationRequestService, atLeastOnce()).delete(MEDICATION_REQUEST_UUID);
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void deleteMedicationRequest_shouldThrowResourceNotFoundException() {
		when(fhirMedicationRequestService.delete(WRONG_MEDICATION_REQUEST_UUID)).thenReturn(null);
		resourceProvider.deleteMedicationRequest(new IdType().setValue(WRONG_MEDICATION_REQUEST_UUID));
		
		verify(fhirMedicationRequestService, atLeastOnce()).delete(WRONG_MEDICATION_REQUEST_UUID);
	}
	
}
