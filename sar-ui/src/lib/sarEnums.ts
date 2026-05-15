export const IDENTIFICATION_TYPES = [
  { code: 'ALIEN_REGISTRATION',  label: 'Alien Registration Number' },
  { code: 'EIN',                 label: 'Employer Identification Number (EIN)' },
  { code: 'DRIVERS_LICENSE',     label: "Driver's License / State ID" },
  { code: 'FOREIGN_ID',          label: 'Foreign Identification' },
  { code: 'SSN_ITIN',            label: 'SSN / ITIN' },
  { code: 'PASSPORT',            label: 'Passport' },
  { code: 'NATIONAL_ID',         label: 'National ID Number' },
  { code: 'OTHER',               label: 'Other' },
  { code: 'UNKNOWN',             label: 'Unknown' },
];

export const SUBJECT_ROLES = [
  { code: 'ACCOUNTANT',    label: 'Accountant' },
  { code: 'AGENT',         label: 'Agent' },
  { code: 'APPRAISER',     label: 'Appraiser' },
  { code: 'ATTORNEY',      label: 'Attorney' },
  { code: 'BORROWER',      label: 'Borrower' },
  { code: 'BROKER_DEALER', label: 'Broker-Dealer' },
  { code: 'CUSTOMER',      label: 'Customer' },
  { code: 'DIRECTOR',      label: 'Director' },
  { code: 'EMPLOYEE',      label: 'Employee' },
  { code: 'LOAN_APPLICANT',label: 'Loan Applicant' },
  { code: 'OFFICER',       label: 'Officer' },
  { code: 'OWNER',         label: 'Owner' },
  { code: 'SHAREHOLDER',   label: 'Shareholder' },
  { code: 'OTHER',         label: 'Other' },
];

export interface ActivityTypeEntry {
  code: string;
  label: string;
  category: string;
}

export const ACTIVITY_TYPE_GROUPS: { label: string; types: ActivityTypeEntry[] }[] = [
  {
    label: 'Fraud',
    types: [
      { code: 'BRIBERY_GRATUITY',            label: 'Bribery/gratuity', category: 'Fraud' },
      { code: 'CHECK_FRAUD',                 label: 'Check fraud', category: 'Fraud' },
      { code: 'CHECK_KITING',                label: 'Check kiting', category: 'Fraud' },
      { code: 'COMMERCIAL_LOAN_FRAUD',       label: 'Commercial loan fraud', category: 'Fraud' },
      { code: 'CONSUMER_LOAN_FRAUD',         label: 'Consumer loan fraud', category: 'Fraud' },
      { code: 'COUNTERFEIT_CHECK',           label: 'Counterfeit check', category: 'Fraud' },
      { code: 'COUNTERFEIT_CREDIT_DEBIT_CARD', label: 'Counterfeit credit/debit card', category: 'Fraud' },
      { code: 'COUNTERFEIT_INSTRUMENT_OTHER', label: 'Counterfeit instrument (other)', category: 'Fraud' },
      { code: 'CREDIT_CARD_FRAUD',           label: 'Credit card fraud', category: 'Fraud' },
      { code: 'DEBIT_CARD_FRAUD',            label: 'Debit card fraud', category: 'Fraud' },
      { code: 'DEFALCATION_EMBEZZLEMENT',    label: 'Defalcation/embezzlement', category: 'Fraud' },
      { code: 'FALSE_STATEMENT',             label: 'False statement', category: 'Fraud' },
      { code: 'MISUSE_OF_POSITION',          label: 'Misuse of position or self-dealing', category: 'Fraud' },
      { code: 'MORTGAGE_LOAN_FRAUD',         label: 'Mortgage loan fraud', category: 'Fraud' },
      { code: 'MYSTERIOUS_DISAPPEARANCE',    label: 'Mysterious disappearance of funds', category: 'Fraud' },
      { code: 'WIRE_TRANSFER_FRAUD',         label: 'Wire transfer fraud', category: 'Fraud' },
      { code: 'FRAUD_OTHER',                 label: 'Fraud (other)', category: 'Fraud' },
    ],
  },
  {
    label: 'Money Laundering',
    types: [
      { code: 'IDENTIFICATION_DOCUMENTATION', label: 'Identification documentation', category: 'Money Laundering' },
      { code: 'MONEY_LAUNDERING',             label: 'Money laundering', category: 'Money Laundering' },
      { code: 'MONEY_LAUNDERING_OTHER',       label: 'Money laundering (other)', category: 'Money Laundering' },
    ],
  },
  {
    label: 'Terrorist Financing',
    types: [
      { code: 'TERRORIST_FINANCING',       label: 'Terrorist financing', category: 'Terrorist Financing' },
      { code: 'TERRORIST_FINANCING_OTHER', label: 'Terrorist financing (other)', category: 'Terrorist Financing' },
    ],
  },
  {
    label: 'Structuring',
    types: [
      { code: 'GAMING_ACTIVITIES',                    label: 'Gaming activities', category: 'Structuring' },
      { code: 'STRUCTURING',                          label: 'Structuring', category: 'Structuring' },
      { code: 'TRANSACTIONS_BELOW_THRESHOLD',         label: 'Transactions below BSA threshold', category: 'Structuring' },
      { code: 'TWO_OR_MORE_INDIVIDUALS',              label: 'Two or more individuals acting together', category: 'Structuring' },
      { code: 'UNUSUAL_MULTIPLE_TRANSACTION_TYPES',   label: 'Unusual use of multiple transaction types', category: 'Structuring' },
      { code: 'STRUCTURING_ML_OTHER',                 label: 'Other structuring/ML', category: 'Structuring' },
    ],
  },
  {
    label: 'Cyber Events / Identity Theft / Other',
    types: [
      { code: 'ACCOUNT_TAKEOVER',          label: 'Account takeover', category: 'Cyber' },
      { code: 'COMPUTER_INTRUSION',        label: 'Computer intrusion/hacking', category: 'Cyber' },
      { code: 'CREDIT_DEBIT_CARD_THEFT',   label: 'Credit/debit card theft', category: 'Cyber' },
      { code: 'DEBIT_CARD_FRAUD_OTHER',    label: 'Debit card fraud (other)', category: 'Cyber' },
      { code: 'ELDER_FINANCIAL_EXPLOITATION', label: 'Elder financial exploitation', category: 'Cyber' },
      { code: 'EMAIL_COMPROMISE',          label: 'Email/business email compromise', category: 'Cyber' },
      { code: 'EXTORTION_BLACKMAIL',       label: 'Extortion/blackmail', category: 'Cyber' },
      { code: 'FALSE_POLICE_REPORT',       label: 'False police report (AML)', category: 'Cyber' },
      { code: 'HOME_EQUITY_FRAUD',         label: 'Home equity fraud', category: 'Cyber' },
      { code: 'HUMAN_TRAFFICKING',         label: 'Human trafficking/smuggling', category: 'Cyber' },
      { code: 'IDENTITY_THEFT',            label: 'Identity theft', category: 'Cyber' },
      { code: 'ILLICIT_MARKETPLACE',       label: 'Illicit marketplace activity', category: 'Cyber' },
      { code: 'INSURANCE_FRAUD',           label: 'Insurance fraud', category: 'Cyber' },
      { code: 'INVESTMENT_FRAUD',          label: 'Investment fraud', category: 'Cyber' },
      { code: 'LOTTERY_SWEEPSTAKES_SCAM',  label: 'Lottery/sweepstakes scam', category: 'Cyber' },
      { code: 'MALWARE_RANSOMWARE',        label: 'Malware/ransomware', category: 'Cyber' },
      { code: 'MASS_MARKETING_FRAUD',      label: 'Mass marketing fraud', category: 'Cyber' },
      { code: 'PONZI_PYRAMID_SCHEME',      label: 'Ponzi/pyramid scheme', category: 'Cyber' },
      { code: 'ROMANCE_FRAUD',             label: 'Romance fraud', category: 'Cyber' },
      { code: 'SECURITIES_FRAUD',          label: 'Securities fraud', category: 'Cyber' },
      { code: 'SOCIAL_ENGINEERING',        label: 'Social engineering', category: 'Cyber' },
      { code: 'TAX_REFUND_FRAUD',          label: 'Tax refund fraud', category: 'Cyber' },
      { code: 'TELEMARKETING_PHONE_FRAUD', label: 'Telemarketing/phone fraud', category: 'Cyber' },
      { code: 'TRADE_BASED_ML',            label: 'Trade-based money laundering', category: 'Cyber' },
    ],
  },
];

export const ALL_ACTIVITY_TYPES: ActivityTypeEntry[] = ACTIVITY_TYPE_GROUPS.flatMap(g => g.types);

export const ACCOUNT_TYPES = [
  'Checking', 'Savings', 'Securities', 'Foreign', 'Crypto', 'Loan', 'Credit Card', 'Other',
];

export const TRANSACTION_TYPES = [
  'Wire transfer', 'ACH', 'Cash deposit', 'Cash withdrawal', 'Check',
  'Credit card', 'Debit card', 'Crypto', 'Peer-to-peer', 'Money order',
  'Cashier check', 'Foreign exchange', 'Other',
];

export const FILING_INSTITUTION_TYPES = [
  'Bank', 'Credit Union', 'Money Services Business', 'Broker-Dealer',
  'Mutual Fund', 'Insurance Company', 'Futures Commission Merchant',
  'Casino', 'Loan or Finance Company', 'Housing Government Sponsored Enterprise',
  'Other',
];

export const US_STATES = [
  { code: 'AL', name: 'Alabama' }, { code: 'AK', name: 'Alaska' }, { code: 'AZ', name: 'Arizona' },
  { code: 'AR', name: 'Arkansas' }, { code: 'CA', name: 'California' }, { code: 'CO', name: 'Colorado' },
  { code: 'CT', name: 'Connecticut' }, { code: 'DE', name: 'Delaware' }, { code: 'FL', name: 'Florida' },
  { code: 'GA', name: 'Georgia' }, { code: 'HI', name: 'Hawaii' }, { code: 'ID', name: 'Idaho' },
  { code: 'IL', name: 'Illinois' }, { code: 'IN', name: 'Indiana' }, { code: 'IA', name: 'Iowa' },
  { code: 'KS', name: 'Kansas' }, { code: 'KY', name: 'Kentucky' }, { code: 'LA', name: 'Louisiana' },
  { code: 'ME', name: 'Maine' }, { code: 'MD', name: 'Maryland' }, { code: 'MA', name: 'Massachusetts' },
  { code: 'MI', name: 'Michigan' }, { code: 'MN', name: 'Minnesota' }, { code: 'MS', name: 'Mississippi' },
  { code: 'MO', name: 'Missouri' }, { code: 'MT', name: 'Montana' }, { code: 'NE', name: 'Nebraska' },
  { code: 'NV', name: 'Nevada' }, { code: 'NH', name: 'New Hampshire' }, { code: 'NJ', name: 'New Jersey' },
  { code: 'NM', name: 'New Mexico' }, { code: 'NY', name: 'New York' }, { code: 'NC', name: 'North Carolina' },
  { code: 'ND', name: 'North Dakota' }, { code: 'OH', name: 'Ohio' }, { code: 'OK', name: 'Oklahoma' },
  { code: 'OR', name: 'Oregon' }, { code: 'PA', name: 'Pennsylvania' }, { code: 'RI', name: 'Rhode Island' },
  { code: 'SC', name: 'South Carolina' }, { code: 'SD', name: 'South Dakota' }, { code: 'TN', name: 'Tennessee' },
  { code: 'TX', name: 'Texas' }, { code: 'UT', name: 'Utah' }, { code: 'VT', name: 'Vermont' },
  { code: 'VA', name: 'Virginia' }, { code: 'WA', name: 'Washington' }, { code: 'WV', name: 'West Virginia' },
  { code: 'WI', name: 'Wisconsin' }, { code: 'WY', name: 'Wyoming' }, { code: 'DC', name: 'District of Columbia' },
  { code: 'XX', name: 'Unknown' },
];
