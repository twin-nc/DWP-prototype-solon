# API Changes

## Added APIs

### Authorization Management API core
- `authorization-management/user-access-groups/search`  
  - Command: `POST`  
  - Action: `Added`
- `authorization-management/user-roles/search`  
  - Command: `POST`  
  - Action: `Added`
- `authorization-management/user-access-groups/users`  
  - Command: `GET`  
  - Action: `Added`
- `authorization-management/roles/search`  
  - Command: `POST`  
  - Action: `Added`
- `authorization-management/user-access-groups/dropdown`  
  - Command: `GET`  
  - Action: `Added`

### Document Management API core
- `document-management/attachments/status/{id}`  
  - Command: `PUT`  
  - Action: `Added`
- `document-management/attachments`  
  - Command: `GET`  
  - Action: `Added`
- `document-management/attachments/configuration/{id}`  
  - Command: `PUT`  
  - Action: `Added`
- `document-management/attachment-related-entities`  
  - Command: `POST`  
  - Action: `Added`
- `document-management/attachments/properties/{id}`  
  - Command: `PUT`  
  - Action: `Added`

### Exception Management API core
- `exception-management/exceptions/search`  
  - Command: `POST`  
  - Action: `Added`

### Reference Data Microservice core
- `reference-data/codelists/{code}/items/item/lowest-sorting`  
  - Command: `GET`  
  - Action: `Added`

### Registration API core
- `registration/users/party-role-id/search`  
  - Command: `POST`  
  - Action: `Added`
- `registration/organizational-unit/v2/search`  
  - Command: `POST`  
  - Action: `Added`
- `registration/workers-availability/list`  
  - Command: `GET`  
  - Action: `Added`
- `registration/users/search`  
  - Command: `POST`  
  - Action: `Added`

### ISS Exception Management API core
- `exception-management/exceptions/search`  
  - Command: `POST`  
  - Action: `Added`

### ISS Reference Data Microservice core
- `reference-data/codelists/{code}/items/item/lowest-sorting`  
  - Command: `GET`  
  - Action: `Added`

### ISS service API core
- `iss-service-management/attachments/status/{id}`  
  - Command: `PUT`  
  - Action: `Added`
- `iss-service-management/attachments`  
  - Command: `GET`  
  - Action: `Added`
- `iss-service-management/attachments/download/{id}`  
  - Command: `GET`  
  - Action: `Added`
- `iss-service-management/attachments/{id}`  
  - Command: `GET`  
  - Action: `Added`

## Modified APIs

### Authorization Management API core
- `authorization-management/securable-objects/{key}`
  - `GET` modified, response `200`, parameter added: `id`
  - `PUT` modified, response `200`, parameter added: `id`
- `authorization-management/roles/{code}/all-securable-objects`
  - `GET` modified, response `200`, parameter added: `id`
- `authorization-management/securable-objects`
  - `POST` modified, response `201`, parameter added: `id`
- `authorization-management/roles/{code}/securable-objects/{key}`
  - `POST` modified, response `201`, parameter added: `id`
  - `PUT` modified, response `200`, parameter added: `id`
- `authorization-management/user-access-groups/user/{id}/assign-user-access-groups`
  - `PUT` modified, response `200`, parameter added: `uniqueId`
- `authorization-management/securable-objects/all`
  - `GET` modified, response `200`, parameter added: `id`

### Document Management API core
- `document-management/attachments/entity/{entityId}`
  - `GET` modified, response `200`, parameters added: `categoryCL`, `policyApplied`, `relationships`, `sourceCL`, `statusCL`, `typeCL`, `uploadedBy`, `dataArea`, `excludedFromSS`
  - `PUT` modified, request body properties added: `categoryCL`, `policyApplied`, `relationships`, `sourceCL`, `statusCL`, `typeCL`, `dataArea`, `excludedFromSS`; response `200` parameters added: `categoryCL`, `policyApplied`, `relationships`, `sourceCL`, `statusCL`, `typeCL`, `uploadedBy`, `dataArea`, `excludedFromSS`
- `document-management/attachments/{id}`
  - `GET` modified, response `200`, parameters added: `categoryCL`, `policyApplied`, `relationships`, `sourceCL`, `statusCL`, `typeCL`, `uploadedBy`, `dataArea`, `excludedFromSS`

### Human Tasks API core
- `human-task-management/human-task-types/{code}/eligible-users`
  - `GET` modified, parameter added: `humanTaskId`

### Message Store API core
- `message-store/messages/{id}`
  - `GET` modified, response `200`, parameter modified: `payload`
- `message-store/messages/{id}/to-status/{status}`
  - `PUT` modified, response `200`, parameter modified: `payload`
- `message-store/messages`
  - `POST` modified, request body property modified: `payload`; response `201`, parameter modified: `payload`

### Notification API core
- `notification/notification-types/{code}`
  - `GET` modified, response `200`, parameters added: `stateCL`, `stateCLDescription`
  - `PUT` modified, request body property added: `stateCL`; response `200`, parameters added: `stateCL`, `stateCLDescription`
- `notification/notification-types`
  - `POST` modified, request body property added: `stateCL`; response `201`, parameters added: `stateCL`, `stateCLDescription`
  - `GET` modified, parameter added: `stateCL`

### Reference Data Microservice core
- `reference-data/codelists/base`
  - `GET` modified, parameters added: `isOmitRefDataMetadata`, `isOmitItems`
- `reference-data/codelists/base/{id}`
  - `GET` modified, parameter added: `isOmitItems`

### Registration API core
- `registration/parties`
  - `POST` modified, request body property added: `additionalInfo`; response `201` parameter added: `additionalInfo`
  - `GET` modified, parameters added: `bankAccountComment`, `individualCountryCL`, `revenueTypeCode`, `revenueTypeRegistrationStatus`, `classificationStartDate`, `address20`, `nameStartDate`, `bankAccountNumber2`, `bankAccountNumber3`, `bankAccountNumber4`, `bankAccountNumber5`, `address7`, `address6`, `address5`, `bankAccountNumber1`, `address4`, `address9`, `address8`, `addressEndDate`, `individualDeathDateFrom`, `individualBirthDateTo`, `stateEndDate`, `currencyCL`, `contactEndDate`, `individualGenderCL`, `preferenceEndDate`, `address10`, `addressCountryCL`, `address11`, `address12`, `address13`, `address14`, `bankAccountStartDate`, `bankAccountFormatCode`, `address15`, `address16`, `address17`, `address18`, `address19`, `individualDeathDateTo`, `revenueTypeRegistrationStartDate`, `bankAccountEndDate`, `accountHolderName`, `contactStartDate`, `addressStartDate`, `bankAccountStatusCL`, `bankCode`, `address3`, `address2`, `address1`, `nameEndDate`, `identifierEndDate`, `addressTypeCL`, `revenueTypeRegistrationEndDate`, `revenueTypeRegistrationPartyRoleId`, `identifierStartDate`, `individualBirthPlace`, `individualBirthDateFrom`, `classificationEndDate`, `preferenceStartDate`
- `registration/parties/{id}`
  - `GET` modified, response `200`, parameter added: `additionalInfo`
  - `PUT` modified, request body property added: `additionalInfo`; response `200`, parameter added: `additionalInfo`
- `registration/users`
  - `GET` modified, parameters added: `userAccessGroupCode`, `partyContactEndDate`, `partyIdentifierValue`, `partyNameTypeCL`, `partyNameStartDate`, `userSubjectId`, `stateEndDate`, `partyIdentifierEndDate`, `userAccessGroupStartDate`, `userId`, `organizationalUnitMemberEndDate`, `partyContactTypeCL`, `partyContactStartDate`, `organizationalUnitMemberStartDate`, `stateStartDate`, `partyNameEndDate`, `userAccessGroupEndDate`, `state`, `partyIdentifierStartDate`, `partyIdentifierTypeCL`, `partyContactValue`, `organizationalUnitMemberIsHead`
- `registration/party-types/{code}`
  - `GET` modified, response `200`, parameter added: `validAdditionalInfoTypes`
  - `PUT` modified, request body property added: `validAdditionalInfoTypes`; response `200`, parameter added: `validAdditionalInfoTypes`
- `registration/users/quick-create`
  - `POST` modified, request body property modified: `keycloakUserExistsSW` (deprecated)
- `registration/assets`
  - `GET` modified, parameters added: `relatedAssetId`, `assetRelationshipEndDate`, `valuationStartDate`, `revenueTypeCode`, `revenueTypeRegistrationStartDate`, `revenueTypeRegistrationStatus`, `classificationStartDate`, `address20`, `assetRelationshipStartDate`, `valuationSource`, `address7`, `address6`, `address5`, `addressStartDate`, `address4`, `assetPartyRoleStartDate`, `valuationEndDate`, `valuationType`, `address9`, `address8`, `addressEndDate`, `assetPartyRoleEndDate`, `assetPartyRoleId`, `address3`, `address2`, `address1`, `stateEndDate`, `identifierEndDate`, `addressTypeCL`, `relatedAssetRelationshipCL`, `revenueTypeRegistrationEndDate`, `revenueTypeRegistrationPartyRoleId`, `address10`, `addressCountryCL`, `identifierStartDate`, `address11`, `valuationStatus`, `address12`, `address13`, `address14`, `address15`, `address16`, `address17`, `classificationEndDate`, `address18`, `valuationReason`, `address19`
  - parameters deleted: `relationshipPartyRoleId`, `assetRelationshipEndDateFrom`, `assetRelationshipStartDateTo`, `assetRelationshipStartDateFrom`, `assetRelationshipEndDateTo`
- `registration/parties/single-party-role-view`
  - `GET` modified, response `200`, parameter added: `additionalInfo`
- `registration/resource-calendars/{code}/exception-calendars/{exceptionCalendarCode}`
  - `PUT` modified, response `200`, parameter added: `id`
- `registration/resource-calendars/{code}/exception-calendars`
  - `POST` modified, response `201`, parameter added: `id`
- `registration/party-roles`
  - `GET` modified, parameters added: `partyAccountHolderName`, `partyBankAccountStartDate`, `partyBankAccountNumber5`, `partyBankAccountNumber4`, `revenueTypeCode`, `assetPartyRoleAssetId`, `revenueTypeRegistrationStatus`, `partyAddressTypeCL`, `address20`, `address7`, `address6`, `address5`, `partyCurrencyCL`, `assetPartyRoleStartDate`, `address4`, `address9`, `address8`, `partyContactValue`, `partyContactEndDate`, `assetPartyRoleEndDate`, `partyNameTypeCL`, `assetPartyRoleRelationshipCL`, `stateEndDate`, `partyAddressStartDate`, `address10`, `addressCountryCL`, `partyBankAccountStatusCL`, `organizationalUnitMemberStartDate`, `address11`, `address12`, `address13`, `partyNameEndDate`, `address14`, `address15`, `address16`, `partyBankAccountComment`, `address17`, `address18`, `address19`, `partyIdentifierStartDate`, `partyRoleRelationshipEndDate`, `revenueTypeRegistrationStartDate`, `organizationalUnitMemberEndDate`, `partyBankCode`, `partyContactType`, `partyAddressEndDate`, `partyRoleRelationshipStartDate`, `partyBankAccountFormatCode`, `address`, `address3`, `partyNameStartDate`, `address2`, `address1`, `partyIdentifierEndDate`, `userId`, `partyBankAccountEndDate`, `revenueTypeRegistrationEndDate`, `relatedPartyRoleFrom`, `partyContactStartDate`, `partyBankAccountNumber1`, `stateStartDate`, `relatedPartyRoleTo`, `partyBankAccountNumber3`, `partyBankAccountNumber2`, `organizationalUnitMemberIsHead`
- `registration/claimants`
  - `GET` modified, parameters added: `partyAddressTypeCL`, `partyContactTypeCL`, `address20`, `address7`, `address6`, `partyAddressEndDate`, `address5`, `address4`, `address9`, `address8`, `partyContactValue`, `partyContactEndDate`, `address`, `partyNameTypeCL`, `address3`, `address2`, `partyNameStartDate`, `address1`, `stateEndDate`, `partyIdentifierEndDate`, `partyAddressStartDate`, `address10`, `addressCountryCL`, `partyContactStartDate`, `address11`, `stateStartDate`, `address12`, `address13`, `address14`, `partyNameEndDate`, `address15`, `address16`, `address17`, `address18`, `address19`, `partyIdentifierStartDate`
- `registration/portal-users/quick-create`
  - `POST` modified, request body property modified: `keycloakUserExistsSW` (deprecated)
- `registration/organizational-unit/v2`
  - `GET` modified, parameters added: `bankAccountComment`, `isHead`, `classificationStartDate`, `address20`, `nameStartDate`, `bankAccountNumber2`, `bankAccountNumber3`, `bankAccountNumber4`, `bankAccountNumber5`, `address7`, `address6`, `address5`, `address4`, `bankAccountNumber1`, `address9`, `address8`, `addressEndDate`, `stateEndDate`, `currencyCL`, `contactEndDate`, `address10`, `address11`, `address12`, `address13`, `address14`, `address15`, `bankAccountStartDate`, `bankAccountFormatCode`, `address16`, `address17`, `userStartDate`, `address18`, `address19`, `bankAccountEndDate`, `accountHolderName`, `contactStartDate`, `nameType`, `addressStartDate`, `countryCL`, `bankCode`, `bankAccountStatusCL`, `address3`, `address2`, `address1`, `nameEndDate`, `identifierEndDate`, `addressTypeCL`, `userEndDate`, `identifierStartDate`, `classificationEndDate`
- `registration/party-types`
  - `POST` modified, request body property added: `validAdditionalInfoTypes`; response `201`, parameter added: `validAdditionalInfoTypes`

### Taxpayer Accounting API core
- `taxpayer-accounting/payment-events`
  - `GET` modified, parameter added: `segmentId`
- `taxpayer-accounting/payment-segments`
  - `GET` modified, parameter added: `segmentId`

### UI Configuration API core
- `ui-configuration/forms/checkform`
  - `POST` modified, parameter added: `formStep`

### Authorization Management API custom
- Same modifications as Authorization Management API core:
  - `authorization-management/securable-objects/{key}` (`GET`/`PUT`, response `200`, `id` added)
  - `authorization-management/roles/{code}/all-securable-objects` (`GET`, response `200`, `id` added)
  - `authorization-management/securable-objects` (`POST`, response `201`, `id` added)
  - `authorization-management/roles/{code}/securable-objects/{key}` (`POST` `201` + `PUT` `200`, `id` added)
  - `authorization-management/user-access-groups/user/{id}/assign-user-access-groups` (`PUT`, response `200`, `uniqueId` added)
  - `authorization-management/securable-objects/all` (`GET`, response `200`, `id` added)

### Human Tasks API custom
- `human-task-management/human-task-types/{code}/eligible-users`
  - `GET` modified, parameter added: `humanTaskId`

### Registration API custom
- Same modifications as Registration API core for:
  - `registration/parties`
  - `registration/parties/{id}`
  - `registration/users`
  - `registration/party-types/{code}`
  - `registration/users/quick-create`
  - `registration/assets`
  - `registration/parties/single-party-role-view`
  - `registration/resource-calendars/{code}/exception-calendars/{exceptionCalendarCode}`
  - `registration/resource-calendars/{code}/exception-calendars`
  - `registration/party-roles`
  - `registration/claimants`
  - `registration/portal-users/quick-create`
  - `registration/organizational-unit/v2`
  - `registration/party-types`

### Taxpayer Accounting API custom
- `taxpayer-accounting/payment-events`
  - `GET` modified, parameter added: `segmentId`
- `taxpayer-accounting/payment-segments`
  - `GET` modified, parameter added: `segmentId`

### ISS Human Tasks API core
- `human-task-management/human-task-types/{code}/eligible-users`
  - `GET` modified, parameter added: `humanTaskId`

### ISS Message Store API core
- `message-store/messages/{id}`
  - `GET` modified, response `200`, parameter modified: `payload`
- `message-store/messages/{id}/to-status/{status}`
  - `PUT` modified, response `200`, parameter modified: `payload`
- `message-store/messages`
  - `POST` modified, request body property modified: `payload`; response `201`, parameter modified: `payload`

### ISS Notification API core
- `notification/notification-types/{code}`
  - `GET` modified, response `200`, parameters added: `stateCL`, `stateCLDescription`
  - `PUT` modified, request body property added: `stateCL`; response `200`, parameters added: `stateCL`, `stateCLDescription`
- `notification/notification-types`
  - `POST` modified, request body property added: `stateCL`; response `201`, parameters added: `stateCL`, `stateCLDescription`
  - `GET` modified, parameter added: `stateCL`

### ISS Reference Data Microservice core
- `reference-data/codelists/base`
  - `GET` modified, parameters added: `isOmitRefDataMetadata`, `isOmitItems`
- `reference-data/codelists/base/{id}`
  - `GET` modified, parameter added: `isOmitItems`

### ISS service API core
- `iss-service-management/forms/operate`
  - `POST` modified, request body property added: `step`; response `200`, parameter added: `step`

### ISS Human Tasks API custom
- `human-task-management/human-task-types/{code}/eligible-users`
  - `GET` modified, parameter added: `humanTaskId`

## Deprecated APIs

### UI Configuration API core
- `ui-configuration/form-types/by-codes`
  - Command: `GET`
  - Action: `Deprecated`

### ISS service API core
- `iss-service-management/forms/checkform`
  - Command: `POST`
  - Action: `Deprecated`
- `iss-service-management/forms/submit`
  - Command: `POST`
  - Action: `Deprecated`
