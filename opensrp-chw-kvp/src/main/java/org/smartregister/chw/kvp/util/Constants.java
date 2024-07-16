package org.smartregister.chw.kvp.util;

public interface Constants {

    int REQUEST_CODE_GET_JSON = 2244;
    String ENCOUNTER_TYPE = "encounter_type";
    String STEP_ONE = "step1";
    String STEP_TWO = "step2";
    String STEP_THREE = "step3";
    String STEP_FOUR = "step4";
    String STEP_FIVE = "step5";
    String STEP_SIX= "step6";
    String STEP_SEVEN = "step7";
    String STEP_EIGHT = "step8";
    String STEP_NINE = "step9";
    String STEP_TEN = "step10";
    String KVP_VISIT_GROUP = "kvp_visit_group";

    String MALE = "male";
    String FEMALE = "female";

    interface JSON_FORM_EXTRA {
        String JSON = "json";
        String ENCOUNTER_TYPE = "encounter_type";
    }

    interface EVENT_TYPE {
        String KVP_PrEP_REGISTRATION = "KVP PrEP Registration";
        String KVP_PrEP_FOLLOW_UP_VISIT = "Kvp PrEP Follow-up Visit";
        String PrEP_FOLLOWUP_VISIT = "PrEP Follow-up Visit";
        String KVP_REGISTRATION = "KVP Registration";
        String PrEP_REGISTRATION = "PrEP Registration";
        String VOID_EVENT = "Void Event";
        String KVP_BIO_MEDICAL_SERVICE_VISIT = "KVP Bio Medical Service Visit";
        String KVP_BEHAVIORAL_SERVICE_VISIT = "KVP Behavioral Service Visit";
        String KVP_STRUCTURAL_SERVICE_VISIT = "KVP Structural Service Visit";
        String KVP_OTHER_SERVICE_VISIT = "KVP Other Service Visit";
        String PrEP_CLIENT_NOT_ELIGIBLE = "PrEP Client Not Eligible";
    }

    interface FORMS {
        String KVP_PrEP_REGISTRATION = "kvp_prep_registration";
        String KVP_FOLLOW_UP_VISIT = "kvp_followup_visit";
        String KVP_SCREENING_MALE = "kvp_screening_male";
        String KVP_SCREENING_FEMALE = "kvp_screening_female";
        String PrEP_REGISTRATION = "prep_registration";
        String HEPATITIS_B_RESULTS = "kvp_hepatitis_b_results";
        String HEPATITIS_C_RESULTS = "kvp_hepatitis_c_results";
    }

    interface KVP_PrEP_FOLLOWUP_FORMS {
        String VISIT_TYPE = "kvp_prep_visit_type";
        String SBCC_SERVICES = "kvp_prep_sbcc_services";
        String PREVENTIVE_SERVICES = "kvp_prep_preventive_services";
        String STRUCTURAL_SERVICES = "kvp_prep_structural_services";
        String REFERRAL_SERVICES = "kvp_prep_referral_services";
    }

    interface KVP_BIO_MEDICAL_SERVICE_FORMS {
        String KVP_CLIENT_STATUS = "kvp_client_status";
        String KVP_CONDOM_PROVISION = "kvp_condom_provision";
        String KVP_HTS = "kvp_hts";
        String KVP_PrEP_PEP = "kvp_prep_pep";
        String KVP_HEPATITIS = "kvp_hepatitis";
        String KVP_STI_SCREENING = "kvp_sti_screening";
        String KVP_MALE_FAMILY_PLANNING_SERVICES = "kvp_male_family_planning_services";
        String KVP_FEMALE_FAMILY_PLANNING_SERVICES = "kvp_female_family_planning_services";
        String KVP_MAT = "kvp_mat";
        String KVP_VMMC_SERVICES = "kvp_vmmc_services";
        String KVP_TB_INVESTIGATION = "kvp_tb_investigation";
        String KVP_CERVICAL_CANCER_SCREENING = "kvp_cervical_cancer_screening";
    }

    interface KVP_BEHAVIORAL_SERVICE_FORMS {
        String KVP_IEC_SBCC = "kvp_iec_sbcc";
        String KVP_HEALTH_EDUCATION = "kvp_health_education";
    }

    interface KVP_STRUCTURAL_AND_OTHER_SERVICES_FORMS {
        String KVP_GBV_ANALYSIS = "kvp_gbv_analysis";
        String KVP_OTHER_SERVICES_REFERRALS = "kvp_other_services_and_referrals";
    }

    interface PrEP_FOLLOWUP_FORMS {
        String VISIT_TYPE = "prep_visit_type";
        String SCREENING = "prep_screening";
        String INITIATION = "prep_initiation";
        String OTHER_SERVICES = "prep_other_services";
    }

    interface TABLES {
        String KVP_PrEP_REGISTER = "ec_kvp_prep_register";
        String KVP_PrEP_FOLLOWUP = "ec_kvp_prep_followup";
        String KVP_REGISTER = "ec_kvp_register";
        String PrEP_REGISTER = "ec_prep_register";
        String KVP_FOLLOW_UP = "ec_kvp_followup";
        String PrEP_FOLLOWUP = "ec_prep_followup";
        String FAMILY_MEMBER_TABLE = "ec_family_member";
        String KVP_HEPATITIS_TEST_RESULTS = "ec_kvp_hepatitis_test_results";
    }

    interface ACTIVITY_PAYLOAD {
        String BASE_ENTITY_ID = "BASE_ENTITY_ID";
        String FAMILY_BASE_ENTITY_ID = "FAMILY_BASE_ENTITY_ID";
        String ACTION = "ACTION";
        String KVP_FORM_NAME = "KVP_FORM_NAME";
        String PARENT_FORM_ENTITY_ID = "PARENT_FORM_ENTITY_ID";
        String MEMBER_PROFILE_OBJECT = "MemberObject";
        String EDIT_MODE = "editMode";
        String GENDER = "gender";
        String AGE = "age";

        String PROFILE_TYPE = "profile_type";
    }

    interface ACTIVITY_PAYLOAD_TYPE {
        String REGISTRATION = "REGISTRATION";
        String FOLLOW_UP_VISIT = "FOLLOW_UP_VISIT";
    }

    interface CONFIGURATION {
        String KVP_CONFIRMATION = "kvp_confirmation";
    }

    interface KVP_MEMBER_OBJECT {
        String MEMBER_OBJECT = "memberObject";
    }

    interface JSON_FORM_KEY {
        String FACILITY_NAME = "facility_name";
        String UIC_ID = "uic_id";
        String CLIENT_GROUP = "client_group";
    }

    interface PROFILE_TYPES {
        String KVP_PROFILE = "kvp_profile";
        String KVP_PrEP_PROFILE = "kvp_prep_profile";
        String PrEP_PROFILE = "prep_profile";
    }

    interface SERVICES {
        String KVP_BIO_MEDICAL = "kvp_bio_medical";
        String KVP_BEHAVIORAL = "kvp_behavioral";
        String KVP_OTHERS = "kvp_others";
        String KVP_STRUCTURAL = "kvp_structural";
    }


}