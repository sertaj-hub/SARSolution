export type SarStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'SUBMITTED' | 'ACKNOWLEDGED' | 'AMENDED';
export type FilingStatus = 'PENDING' | 'GENERATING' | 'GENERATED' | 'SUBMITTED' | 'ACCEPTED' | 'REJECTED' | 'FAILED';

export const ALL_SAR_STATUSES: SarStatus[] = ['DRAFT', 'IN_REVIEW', 'APPROVED', 'SUBMITTED', 'ACKNOWLEDGED', 'AMENDED'];

export interface SarSubjectDto {
  id?: string;
  seqNum?: number;
  caseSubjectId?: string;
  autoPopulated?: boolean;
  isEntity?: boolean;
  lastName?: string;
  firstName?: string;
  middleName?: string;
  suffix?: string;
  dateOfBirth?: string;
  entityName?: string;
  doingBusinessAs?: string;
  idType?: string;
  idNumber?: string;
  idIssueState?: string;
  idIssueCountry?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
  phoneNumber?: string;
  phoneExtension?: string;
  email?: string;
  occupation?: string;
  naicsCode?: string;
  roleCode?: string;
  roleOtherDescription?: string;
  isUnknown?: boolean;
  stillEmployed?: boolean;
  correctiveAction?: string;
  hasRelationshipToAccount?: boolean;
}

export interface SarAccountDto {
  id?: string;
  subjectId?: string;
  caseAccountId?: string;
  autoPopulated?: boolean;
  accountNumber?: string;
  accountNumberClosed?: boolean;
  accountType?: string;
  productType?: string;
  institutionName?: string;
  institutionEin?: string;
  routingNumber?: string;
  openedDate?: string;
  closedDate?: string;
  balance?: number;
  currencyCode?: string;
  isForeignAccount?: boolean;
  foreignCountry?: string;
  actionAccountClosed?: boolean;
  actionAccountFrozen?: boolean;
  noActionTaken?: boolean;
}

export interface SarTransactionDto {
  id?: string;
  accountId?: string;
  caseTransactionId?: string;
  alertId?: string;
  autoPopulated?: boolean;
  transactionDate?: string;
  transactionDatetime?: string;
  transactionType?: string;
  transactionTypeOther?: string;
  amount?: number;
  currencyCode?: string;
  currencyForeign?: string;
  direction?: string;
  counterpartyName?: string;
  counterpartyAccount?: string;
  counterpartyInstitution?: string;
  counterpartyCountry?: string;
  locationType?: string;
  locationDescription?: string;
  referenceNumber?: string;
  checkNumber?: string;
  structuringFlag?: boolean;
  rapidMovementFlag?: boolean;
  unusualPatternFlag?: boolean;
  suspicionNotes?: string;
}

export interface SarActivityTypeDto {
  id?: string;
  seqNum?: number;
  activityTypeCode?: string;
  activityTypeOther?: string;
  amount?: number;
  productInstrumentDescription?: string;
  productType?: string;
}

export interface SarBranchDto {
  id?: string;
  seqNum?: number;
  branchName?: string;
  rssdNumber?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: string;
  isPrimary?: boolean;
}

export interface SarReportDto {
  id: string;
  reportNumber: string;
  caseId?: string;
  caseSystem?: string;
  status: SarStatus;
  filingStatus?: string;
  activityFromDate?: string;
  activityToDate?: string;
  filingDate?: string;
  continuingActivity?: boolean;
  correctsAmendsPrior?: boolean;
  jointReport?: boolean;
  fiNotedSuspiciousActivity?: boolean;
  totalSuspiciousAmount?: number;
  noAmountInvolved?: boolean;
  narrative?: string;
  filingInstitutionName: string;
  filingInstitutionEin?: string;
  filingInstitutionType?: string;
  contactOfficeName?: string;
  contactPhone?: string;
  contactEmail?: string;
  priorBsaIdentifier?: string;
  bsaIdentifier?: string;
  fincenTrackingNumber?: string;
  submittedAt?: string;
  acknowledgedAt?: string;
  subjects: SarSubjectDto[];
  accounts: SarAccountDto[];
  transactions: SarTransactionDto[];
  activityTypes: SarActivityTypeDto[];
  branches: SarBranchDto[];
  createdAt: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface CreateSarRequest {
  filingInstitutionName: string;
  filingInstitutionEin?: string;
  filingInstitutionType?: string;
  contactOfficeName?: string;
  contactPhone?: string;
  contactEmail?: string;
  caseId?: string;
  caseSystem?: string;
  activityFromDate?: string;
  activityToDate?: string;
  totalSuspiciousAmount?: number;
  continuingActivity?: boolean;
  jointReport?: boolean;
  narrative?: string;
  autoPopulateFromCase?: boolean;
}

export interface AuditLogDto {
  id: string;
  sarReportId?: string;
  entityType: string;
  entityId?: string;
  action: string;
  fieldName?: string;
  oldValue?: string;
  newValue?: string;
  performedBy: string;
  performedAt: string;
  ipAddress?: string;
  reason?: string;
  additionalData?: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface EFilingBatch {
  id: string;
  batchNumber: string;
  transmitterName: string;
  transmitterEin: string;
  status: FilingStatus;
  activityCount: number;
  generatedAt?: string;
  submittedAt?: string;
  acknowledgedAt?: string;
  acknowledgmentStatus?: string;
  fincenTrackingId?: string;
  errorDescription?: string;
  createdAt: string;
}

export interface XmlValidationResult {
  valid: boolean;
  skipped: boolean;
  skipReason?: string;
  errors: string[];
  warnings: string[];
  schemas?: Record<string, string>;
}
