package org.splittydupe.startup.service;

import org.splittydupe.startup.exception.OcrProcessingException;
import org.splittydupe.startup.model.Receipt;
import org.splittydupe.startup.model.LineItem;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.cloud.documentai.v1.Document;
import com.google.protobuf.ByteString;
import com.google.common.base.Strings;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

@Slf4j
@Service
public class OcrService {

    @Value("${GCP_PROJECT_ID}")
    private String projectId;

    @Value("${PROCESSOR_ID}")
    private String processorId;

    @Value("${PROCESSOR_LOCATION}")
    private String processorLocation;

    private DocumentProcessorServiceClient documentProcessorServiceClient;

    @PostConstruct
    public void initReceiptParser() {
        try {
            documentProcessorServiceClient = DocumentProcessorServiceClient.create();
            log.info("Document AI client initialized successfully");
        } catch (IOException e) {
            log.error("Failed to initialize DocumentProcessorServiceClient", e);
            throw new OcrProcessingException("Failed to initialize DocumentProcessorServiceClient", e);
        }
    }

    public Receipt processReceiptImage(String filePath) {
        log.info("Processing receipt image: {}", filePath);
        ProcessResponse response = executeOcrProcessing(filePath);
        Receipt receipt = extractReceiptData(response);
        log.info("Successfully processed receipt. UID: {}", receipt.getUid());
        return receipt;
    }

    private ProcessResponse executeOcrProcessing(String filePath) {
        try {
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);
            String mimeType = Files.probeContentType(path);

            RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.copyFrom(fileContent))
                    .setMimeType(mimeType)
                    .build();

            String processorName = String.format("projects/%s/locations/%s/processors/%s",
                    projectId, processorLocation, processorId);

            ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(processorName)
                    .setRawDocument(rawDocument)
                    .build();

            ProcessResponse response = documentProcessorServiceClient.processDocument(request);
            log.debug("OCR processing completed for file: {}", filePath);
            return response;
        } catch (IOException e) {
            log.error("Failed to read or process receipt file: {}", filePath, e);
            throw new OcrProcessingException("Failed to read or process receipt file: " + filePath, e);
        }
    }

    private Receipt extractReceiptData(ProcessResponse response) {
        Document document = response.getDocument();
        Receipt receipt = new Receipt();

        receipt.setUid(java.util.UUID.randomUUID().toString());
        receipt.setLineItems(new ArrayList<>());

        for (Document.Entity entity : document.getEntitiesList()) {
            String type = entity.getType();
            switch (type) {
                case "supplier_name":
                case "vendor_name":
                    receipt.setVendor(entity.getMentionText());
                    break;
                case "receipt_date":
                    if (entity.hasNormalizedValue()) {
                        receipt.setReceiptDate(entity.getNormalizedValue().getText());
                    }
                    break;
                case "currency":
                    if (entity.hasNormalizedValue()) {
                        receipt.setCurrency(entity.getNormalizedValue().getText());
                    }
                    break;
                case "net_amount":
                    if (entity.hasNormalizedValue()) {
                        receipt.setSubtotal(Double.parseDouble(entity.getNormalizedValue().getText()));
                    }
                    break;
                case "total_tax_amount":
                    if (entity.hasNormalizedValue()) {
                        receipt.setTax(Double.parseDouble(entity.getNormalizedValue().getText()));
                    }
                    break;
                case "tip_amount":
                    if (entity.hasNormalizedValue()) {
                        receipt.setTip(Double.parseDouble(entity.getNormalizedValue().getText()));
                    }
                    break;
                case "total_amount":
                    if (entity.hasNormalizedValue()) {
                        receipt.setTotal(Double.parseDouble(entity.getNormalizedValue().getText()));
                    }
                    break;
                case "line_item":
                    LineItem lineItem = extractLineItem(entity);
                    if (lineItem != null) {
                        receipt.getLineItems().add(lineItem);
                    }
                    break;
            }
        }

        calculatePercentages(receipt);
        return receipt;
    }

    private LineItem extractLineItem(Document.Entity entity) {
        LineItem lineItem = new LineItem();
        for (Document.Entity property : entity.getPropertiesList()) {
            String propertyType = property.getType();
            if (propertyType.equals("line_item/description")) {
                lineItem.setName(property.getMentionText());
            } else if (propertyType.equals("line_item/amount") && property.hasNormalizedValue()) {
                lineItem.setPrice(Double.parseDouble(property.getNormalizedValue().getText()));
            } else if (propertyType.equals("line_item/quantity") && property.hasNormalizedValue()) {
                lineItem.setQuantity(Integer.parseInt(property.getNormalizedValue().getText()));
            }
        }

        if (!Strings.isNullOrEmpty(lineItem.getName()) && lineItem.getPrice() != 0) {
            return lineItem;
        }
        return null;
    }

    private void calculatePercentages(Receipt receipt) {
        if (receipt.getSubtotal() > 0) {
            double taxPercentage = (receipt.getTax() / receipt.getSubtotal()) * 100;
            receipt.setTaxPercentage(taxPercentage);

            double tipPercentage = (receipt.getTip() / receipt.getSubtotal()) * 100;
            receipt.setTipPercentage(tipPercentage);
        }
    }
}
