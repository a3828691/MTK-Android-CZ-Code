LOCAL_DIR := $(GET_LOCAL_DIR)
TARGET := k37tv1_64_bsp
MODULES += app/mt_boot \
           dev/lcm
MTK_FAN5405_SUPPORT = yes
#MTK_BQ24296_SUPPORT = no
#DEFINES += MTK_BQ24296_SUPPORT
MTK_DISABLE_POWER_ON_OFF_VOLTAGE_LIMITATION = yes
MTK_EMMC_SUPPORT = yes
DEFINES += MTK_NEW_COMBO_EMMC_SUPPORT
MTK_KERNEL_POWER_OFF_CHARGING = yes
MTK_PUMP_EXPRESS_SUPPORT := no
MTK_LCM_PHYSICAL_ROTATION = 0
CUSTOM_LK_LCM="jd9366_dsi_wsvga_vdo"
#CUSTOM_LK_LCM="r63417_fhd_dsi_cmd_truly_nt50358"
#hx8392a_dsi_cmd = yes
#DEFINES += MTK_LCM_DEVICE_TREE_SUPPORT
MTK_SECURITY_SW_SUPPORT = yes
MTK_VERIFIED_BOOT_SUPPORT = yes
MTK_SEC_FASTBOOT_UNLOCK_SUPPORT = yes
DEBUG := 0
BOOT_LOGO:=wxga
#DEFINES += WITH_DEBUG_DCC=1
DEFINES += WITH_DEBUG_UART=1
#DEFINES += WITH_DEBUG_FBCON=1
#DEFINES += MACH_FPGA=y
#DEFINES += SB_LK_BRINGUP=y
DEFINES += MTK_GPT_SCHEME_SUPPORT
MTK_GOOGLE_TRUSTY_SUPPORT = no
MTK_DM_VERITY_OFF = yes
MTK_DYNAMIC_CCB_BUFFER_GEAR_ID =
