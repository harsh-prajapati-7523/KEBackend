package com.ke.ticketsystemke.entity;
public enum WarrantyAttachmentCategory{
 BILL_IMAGE(false,true),WARRANTY_CARD_IMAGE(false,true),PRODUCT_IMAGE(false,false),PRODUCT_SERIAL_NUMBER_IMAGE(false,true),MANUFACTURER_SERVICE_SLIP(false,true),OTHER_WARRANTY_DOCUMENT(false,false),
 REPLACEMENT_PROOF(true,true),REPLACEMENT_PRODUCT_IMAGE(true,false),REPLACEMENT_SERIAL_NUMBER_IMAGE(true,true),REPLACEMENT_WARRANTY_CARD_IMAGE(true,true),OTHER_REPLACEMENT_DOCUMENT(true,false);
 private final boolean replacement;private final boolean single;WarrantyAttachmentCategory(boolean replacement,boolean single){this.replacement=replacement;this.single=single;}public boolean isReplacement(){return replacement;}public boolean isSingleCurrent(){return single;}
}
