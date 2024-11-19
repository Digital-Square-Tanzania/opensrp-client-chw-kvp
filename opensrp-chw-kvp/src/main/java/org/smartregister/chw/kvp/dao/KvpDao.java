package org.smartregister.chw.kvp.dao;

import org.apache.commons.lang3.StringUtils;
import org.smartregister.chw.kvp.domain.MemberObject;
import org.smartregister.chw.kvp.util.Constants;
import org.smartregister.dao.AbstractDao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class KvpDao extends AbstractDao {


    public static boolean isRegisteredForKvpPrEP(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_kvp_prep_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static boolean hasTestResults(String baseEntityID) {
        String sql = "SELECT  test_type  FROM " + Constants.TABLES.KVP_HEPATITIS_TEST_RESULTS + " WHERE entity_id = '" + baseEntityID + "' AND test_type IS NOT NULL ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> testTypeDataMap = cursor -> getCursorValue(cursor, "test_type");

        List<String> testTypeRes = readData(sql, testTypeDataMap);

        return testTypeRes != null && !testTypeRes.isEmpty();
    }

    public static boolean hasPrepTestResults(String baseEntityID) {
        String sql = "SELECT  test_type  FROM " + Constants.TABLES.PREP_HEPATITIS_AND_CRCL_TEST_RESULTS + " WHERE entity_id = '" + baseEntityID + "' AND test_type IS NOT NULL ORDER BY last_interacted_with DESC LIMIT 1";

        DataMap<String> testTypeDataMap = cursor -> getCursorValue(cursor, "test_type");

        List<String> testTypeRes = readData(sql, testTypeDataMap);

        return testTypeRes != null && !testTypeRes.isEmpty();
    }

    public static boolean isRegisteredForKvp(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_kvp_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return false;

        return res.get(0) > 0;
    }

    public static boolean isRegisteredForPrEP(String baseEntityID) {
        String sql = "SELECT count(p.base_entity_id) count FROM ec_prep_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.agreed_to_use_prep = 'yes'";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "count");

        List<Integer> res = readData(sql, dataMap);
        if (res != null && !res.isEmpty() && res.get(0) != null) {
            return res.get(0) > 0;
        }
        return false;
    }

    public static boolean isClientClosed(String baseEntityId, String profileType) {

        String tableName = profileType.equals(Constants.PROFILE_TYPES.PrEP_PROFILE)
                ? Constants.TABLES.PrEP_REGISTER
                : profileType.equals(Constants.PROFILE_TYPES.KVP_PROFILE)
                ? Constants.TABLES.KVP_REGISTER
                : Constants.TABLES.KVP_PrEP_REGISTER;

        String sql = "SELECT p.is_closed FROM " + tableName + " p " +
                "WHERE p.base_entity_id = '" + baseEntityId + "'";

        DataMap<Integer> dataMap = cursor -> getCursorIntValue(cursor, "is_closed");

        List<Integer> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0) == 1;
        }
        return false;
    }

    public static boolean isClientEligibleForPrEPFromScreening(String baseEntityID) {
        String sql = "SELECT prep_qualified FROM ec_kvp_register p " +
                "WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_qualified");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0).equalsIgnoreCase("yes");
        }
        return false;
    }

    public static boolean isClientHTSResultsNegative(String baseEntityID) {
        String sql = "SELECT hiv_status FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityID + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "hiv_status");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0).equalsIgnoreCase("negative");
        }
        return false;
    }

    public static MemberObject getMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_kvp_prep_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static MemberObject getKvpMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id as family_base_entity_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_kvp_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "family_base_entity_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "family_base_entity_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static MemberObject getPrEPMember(String baseEntityID) {
        String sql = "select m.base_entity_id,\n" +
                "       m.unique_id,\n" +
                "       m.relational_id as family_base_entity_id,\n" +
                "       m.dob,\n" +
                "       m.first_name,\n" +
                "       m.middle_name,\n" +
                "       m.last_name,\n" +
                "       m.gender,\n" +
                "       m.phone_number,\n" +
                "       m.other_phone_number,\n" +
                "       f.first_name     family_name,\n" +
                "       f.primary_caregiver,\n" +
                "       f.family_head,\n" +
                "       f.village_town,\n" +
                "       fh.first_name    family_head_first_name,\n" +
                "       fh.middle_name   family_head_middle_name,\n" +
                "       fh.last_name     family_head_last_name,\n" +
                "       fh.phone_number  family_head_phone_number,\n" +
                "       ancr.is_closed   anc_is_closed,\n" +
                "       pncr.is_closed   pnc_is_closed,\n" +
                "       pcg.first_name   pcg_first_name,\n" +
                "       pcg.last_name    pcg_last_name,\n" +
                "       pcg.middle_name  pcg_middle_name,\n" +
                "       pcg.phone_number pcg_phone_number,\n" +
                "       mr.*\n" +
                "from ec_family_member m\n" +
                "         inner join ec_family f on m.relational_id = f.base_entity_id\n" +
                "         inner join ec_prep_register mr on mr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_family_member fh on fh.base_entity_id = f.family_head\n" +
                "         left join ec_family_member pcg on pcg.base_entity_id = f.primary_caregiver\n" +
                "         left join ec_anc_register ancr on ancr.base_entity_id = m.base_entity_id\n" +
                "         left join ec_pregnancy_outcome pncr on pncr.base_entity_id = m.base_entity_id\n" +
                "where m.base_entity_id = '" + baseEntityID + "' ";
        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

        DataMap<MemberObject> dataMap = cursor -> {
            MemberObject memberObject = new MemberObject();

            memberObject.setFirstName(getCursorValue(cursor, "first_name", ""));
            memberObject.setMiddleName(getCursorValue(cursor, "middle_name", ""));
            memberObject.setLastName(getCursorValue(cursor, "last_name", ""));
            memberObject.setAddress(getCursorValue(cursor, "village_town"));
            memberObject.setGender(getCursorValue(cursor, "gender"));
            memberObject.setUniqueId(getCursorValue(cursor, "unique_id", ""));
            memberObject.setDob(getCursorValue(cursor, "dob"));
            memberObject.setFamilyBaseEntityId(getCursorValue(cursor, "family_base_entity_id", ""));
            memberObject.setRelationalId(getCursorValue(cursor, "relational_id", ""));
            memberObject.setPrimaryCareGiver(getCursorValue(cursor, "primary_caregiver"));
            memberObject.setFamilyName(getCursorValue(cursor, "family_name", ""));
            memberObject.setPhoneNumber(getCursorValue(cursor, "phone_number", ""));
            memberObject.setKvpTestDate(getCursorValueAsDate(cursor, "kvp_test_date", df));
            memberObject.setBaseEntityId(getCursorValue(cursor, "base_entity_id", ""));
            memberObject.setFamilyHead(getCursorValue(cursor, "family_head", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "pcg_phone_number", ""));
            memberObject.setFamilyHeadPhoneNumber(getCursorValue(cursor, "family_head_phone_number", ""));
            memberObject.setAncMember(getCursorValue(cursor, "anc_is_closed", ""));
            memberObject.setPncMember(getCursorValue(cursor, "pnc_is_closed", ""));

            String familyHeadName = getCursorValue(cursor, "family_head_first_name", "") + " "
                    + getCursorValue(cursor, "family_head_middle_name", "");

            familyHeadName =
                    (familyHeadName.trim() + " " + getCursorValue(cursor, "family_head_last_name", "")).trim();
            memberObject.setFamilyHeadName(familyHeadName);

            String familyPcgName = getCursorValue(cursor, "pcg_first_name", "") + " "
                    + getCursorValue(cursor, "pcg_middle_name", "");

            familyPcgName =
                    (familyPcgName.trim() + " " + getCursorValue(cursor, "pcg_last_name", "")).trim();
            memberObject.setPrimaryCareGiverName(familyPcgName);

            return memberObject;
        };

        List<MemberObject> res = readData(sql, dataMap);
        if (res == null || res.size() != 1)
            return null;

        return res.get(0);
    }

    public static String getUIC_ID(String baseEntityId, String tableName) {
        String sql = "SELECT uic_id FROM " + tableName + " p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "uic_id");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static String getDominantKVPGroup(String baseEntityId) {
        String sql = "SELECT client_group FROM ec_kvp_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "client_group");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0);
        }
        return "";
    }

    public static List<String> getOtherKvpGroups(String baseEntityId) {
        String sql = "SELECT other_kvp_category FROM ec_kvp_bio_medical_services p " +
                " WHERE p.entity_id = '" + baseEntityId + "' ORDER by date(substr(p.kvp_visit_date, 7, 4) || '-' || substr(p.kvp_visit_date, 4, 2) || '-' || substr(p.kvp_visit_date, 1, 2)) DESC LIMIT 1";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "other_kvp_category");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            try {
                String kvpGroups = res.get(0);
                List<String> otherKvpGroups = new ArrayList<>();
                if (kvpGroups != null && kvpGroups.length() > 0) {
                    if (kvpGroups.startsWith("[") && kvpGroups.endsWith("]")) {
                        kvpGroups = kvpGroups.substring(1, kvpGroups.length() - 1); // Remove the brackets
                        String[] elements = kvpGroups.split(",\\s*"); // Split the string using comma followed by optional whitespace
                        otherKvpGroups = Arrays.asList(elements);
                    } else {
                        otherKvpGroups.add(kvpGroups); // Add the input string as a single element to the list
                    }
                }

                return otherKvpGroups;
            } catch (Exception e) {

            }
        }
        return null;
    }

    public static boolean isPrEPInitiated(String baseEntityId) {
        String sql = "SELECT prep_status FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_status");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return StringUtils.isNotBlank(res.get(0)) && !(res.get(0).equalsIgnoreCase("not_initiated") || res.get(0).equalsIgnoreCase("discontinued_quit"));
        }
        return false;
    }

    public static String getPrepInitiationDate(String baseEntityId) {
        String sql = "SELECT prep_initiation_date FROM ec_prep_register p " +
                " WHERE p.base_entity_id = '" + baseEntityId + "' AND p.is_closed = 0 ";

        DataMap<String> dataMap = cursor -> getCursorValue(cursor, "prep_initiation_date");

        List<String> res = readData(sql, dataMap);
        if (res != null && res.size() != 0 && res.get(0) != null) {
            return res.get(0);
        }
        return null;
    }

    public static String getSyncLocationId(String baseEntityId) {
        try {
            String sql = String.format("SELECT sync_location_id FROM ec_family_member WHERE base_entity_id = '%s'", baseEntityId);
            DataMap<String> dataMap = cursor -> getCursorValue(cursor, "sync_location_id");
            List<String> res = readData(sql, dataMap);

            if (res == null || res.size() != 1)
                return null;

            return res.get(0);
        } catch (Exception e) {
            Timber.e(e);
            return null;
        }
    }
}
