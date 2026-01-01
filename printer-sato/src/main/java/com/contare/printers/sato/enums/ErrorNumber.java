package com.contare.printers.sato.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ErrorNumber {

    ONLINE(0, "EN00", "00: Online - Not an error. Return is performed"),
    OFFLINE(1, "EN01", "01: Offline - Not an error. Return is performed"),
    MACHINE_ERROR(2, "EN02", "02: Machine error"),
    MEMORY_ERROR(3, "EN03", "03: Memory error"),
    PROGRAM_ERROR(4, "EN04", "04: Program error"),
    SETTING_INFO_FLASH_ERROR(5, "EN05", "05: Setting information error (FLASH-ROM error)"),
    SETTING_INFO_EEPROM_ERROR(6, "EN06", "06: Setting information error (EE-PROM error)"),
    DOWNLOAD_ERROR(7, "EN07", "07: Download error"),
    PARITY_ERROR(8, "EN08", "08: Parity error"),
    OVER_RUN(9, "EN09", "09: Over run"),
    FRAMING_ERROR(10, "EN10", "10: Framing error"),
    LAN_TIMEOUT_ERROR(11, "EN11", "11: LAN timeout error"),
    BUFFER_OVER(12, "EN12", "12: Buffer over"),
    HEAD_OPEN(13, "EN13", "13: Head open"),
    PAPER_END(14, "EN14", "14: Paper end"),
    RIBBON_END(15, "EN15", "15: Ribbon end"),
    MEDIA_ERROR(16, "EN16", "16: Media error"),
    SENSOR_ERROR(17, "EN17", "17: Sensor error"),
    PRINTHEAD_ERROR(18, "EN18", "18: Printhead error"),
    COVER_OPEN_ERROR(19, "EN19", "19: Cover open error"),
    MEMORY_CARD_TYPE_ERROR(20, "EN20", "20: Memory/Card type error"),
    MEMORY_CARD_READ_WRITE_ERROR(21, "EN21", "21: Memory/Card read/write error"),
    MEMORY_CARD_FULL_ERROR(22, "EN22", "22: Memory/Card full error"),
    MEMORY_CARD_NO_BATTERY_ERROR(23, "EN23", "23: Memory/Card no battery error"),
    RIBBON_SAVER_ERROR(24, "EN24", "24: Ribbon saver error"),
    CUTTER_ERROR(25, "EN25", "25: Cutter error"),
    CUTTER_SENSOR_ERROR(26, "EN26", "26: Cutter sensor error"),
    STACKER_FULL_ERROR(27, "EN27", "27: Stacker full error"),
    COMMAND_ERROR(28, "EN28", "28: Command error"),
    SENSOR_ERROR_AT_POWER_ON(29, "EN29", "29: Sensor error at Power-On"),
    RFID_TAG_ERROR(30, "EN30", "30: RFID tag error"),
    INTERFACE_CARD_ERROR(31, "EN31", "31: Interface card error"),
    REWINDER_ERROR(32, "EN32", "32: Rewinder error"),
    OTHER_ERROR(33, "EN33", "33: Other error"),
    RFID_CONTROL_ERROR(34, "EN34", "34: RFID control error"),
    HEAD_DENSITY_ERROR(35, "EN35", "35: Head density error"),
    KANJI_DATA_ERROR(36, "EN36", "36: Kanji data error"),
    CALENDAR_ERROR(37, "EN37", "37: Calendar error"),
    ITEM_NO_ERROR(38, "EN38", "38: Item No error"),
    BCC_ERROR(39, "EN39", "39: BCC error"),
    CUTTER_COVER_OPEN_ERROR(40, "EN40", "40: Cutter cover open error"),
    RIBBON_REWIND_NON_LOCK_ERROR(41, "EN41", "41: Ribbon rewind non-lock error"),
    COMMUNICATION_TIMEOUT_ERROR(42, "EN42", "42: Communication timeout error"),
    LID_LATCH_OPEN_ERROR(43, "EN43", "43: Lid latch open error"),
    NO_MEDIA_ERROR_AT_POWER_ON(44, "EN44", "44: No media error at Power-On"),
    SD_CARD_ACCESS_ERROR(45, "EN45", "45: SD card access error"),
    SD_CARD_FULL_ERROR(46, "EN46", "46: SD card full error"),
    HEAD_LIFT_ERROR(47, "EN47", "47: Head lift error"),
    HEAD_OVERHEAT_ERROR(48, "EN48", "48: Head overheat error"),
    SNTP_TIME_CORRECTION_ERROR(49, "EN49", "49: SNTP time correction error"),
    CRC_ERROR(50, "EN50", "50: CRC error"),
    CUTTER_MOTOR_ERROR(51, "EN51", "51: Cutter motor error"),
    WLAN_MODULE_ERROR(52, "EN52", "52: WLAN module error"),
    SCANNER_READING_ERROR(53, "EN53", "53: Scanner reading error"),
    SCANNER_CHECKING_ERROR(54, "EN54", "54: Scanner checking error"),
    SCANNER_CONNECTION_ERROR(55, "EN55", "55: Scanner connection error"),
    BLUETOOTH_MODULE_ERROR(56, "EN56", "56: BluetoothModule error"),
    EAP_AUTHENTICATION_FAILED(57, "EN57", "57: EAP authentication error (EAP failed)"),
    EAP_AUTHENTICATION_TIMEOUT(58, "EN58", "58: EAP authentication error (Time out)"),
    BATTERY_ERROR(59, "EN59", "59: Battery error"),
    LOW_BATTERY(60, "EN60", "60: Low Battery error"),
    LOW_BATTERY_CHARGING(61, "EN61", "61: Low Battery error (Charging)"),
    BATTERY_NOT_INSTALLED(62, "EN62", "62: Battery not installed error"),
    BATTERY_TEMPERATURE_ERROR(63, "EN63", "63: Battery temperature error"),
    BATTERY_DETERIORATION_ERROR(64, "EN64", "64: Battery deterioration error"),
    MOTOR_TEMPERATURE_ERROR(65, "EN65", "65: Motor temperature error"),
    INSIDE_CHASSIS_TEMPERATURE_ERROR(66, "EN66", "66: Inside chassis temperature error"),
    JAM_ERROR(67, "EN67", "67: Jam error"),
    SIPL_FIELD_FULL_ERROR(68, "EN68", "68: SIPL Field full error"),
    POWER_OFF_WHEN_CHARGING_ERROR(69, "EN69", "69: Power off error when charging"),
    WLAN_MODULE_ERROR_DUPLICATE(70, "EN70", "70: WLAN module error (duplicate entry in table)"),
    OPTION_MISMATCH_ERROR(71, "EN71", "71: Option mismatch error"),
    BATTERY_DETERIORATION_NOTICE(72, "EN72", "72: Battery deterioration error (Notice)"),
    BATTERY_DETERIORATION_WARNING(73, "EN73", "73: Battery deterioration error (Warning)"),
    POWER_OFF_ERROR(74, "EN74", "74: Power off error"),
    NONRFID_WARNING_ERROR(75, "EN75", "75: NonRFID Warning Error"),
    BARCODE_READER_CONNECTION_ERROR(76, "EN76", "76: Barcode reader connection error"),
    BARCODE_READING_ERROR(77, "EN77", "77: Barcode reading error"),
    BARCODE_VERIFICATION_ERROR(78, "EN78", "78: Barcode verification error"),
    BARCODE_READING_VERIFICATION_POSITION_ERROR(79, "EN79", "79: Barcode reading error (Verification start position abnormality)");

    private final int number;
    private final String code;
    private final String description;


    private static final Map<Integer, ErrorNumber> _numbers = new HashMap<>();
    private static final Map<String, ErrorNumber> _codes = new HashMap<>();

    static {
        for (ErrorNumber row : values()) {
            _numbers.put(row.number, row);
            _codes.put(row.code, row);
        }
    }

    public static ErrorNumber get(final Integer value) {
        if (value == null) return null;
        return _numbers.get(value);
    }

    public static ErrorNumber get(final String value) {
        if (value == null) return null;
        return _codes.get(value);
    }

    @Override
    public String toString() {
        return String.format("%s: %s", code, description);
    }

}
