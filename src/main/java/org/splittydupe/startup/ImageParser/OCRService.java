package org.splittydupe.startup.ImageParser;

import org.splittydupe.startup.Database.Receipt;
import org.splittydupe.startup.Database.LineItem;

import com.google.cloud.documentai.v1.DocumentProcessorServiceClient;
import com.google.cloud.documentai.v1.ProcessRequest;
import com.google.cloud.documentai.v1.ProcessResponse;
import com.google.cloud.documentai.v1.RawDocument;
import com.google.cloud.documentai.v1.Document;
import com.google.protobuf.ByteString;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OCRService {
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
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize DocumentProcessorServiceClient", e);
        }
    }

    public Receipt wrapper(String filePath) {
        ProcessResponse response = processReceiptImage(filePath);
        Receipt receipt = extractReceiptData(response);
        return receipt;
    }

    private ProcessResponse processReceiptImage(String filePath) {
        try {
            Path path = Paths.get(filePath);
            byte[] fileContent = Files.readAllBytes(path);
            String mimeType = Files.probeContentType(path);

            RawDocument rawDocument = RawDocument.newBuilder().setContent(ByteString.copyFrom(fileContent)).setMimeType(mimeType).build();
            String processorName = String.format("projects/%s/locations/%s/processors/%s", projectId, processorLocation, processorId);

            ProcessRequest request = ProcessRequest.newBuilder().setName(processorName).setRawDocument(rawDocument).build();

            ProcessResponse response = documentProcessorServiceClient.processDocument(request);

            // // Write to json file in output directory for local testing
            // Path parentPath = path.getParent();
            // Path outputPath = parentPath.resolve("outputInvoice");
            // String fileNameWithExtension = path.getFileName().toString();
            // int extensionIndex = fileNameWithExtension.lastIndexOf('.');
            // String fileName = (extensionIndex == -1) ? fileNameWithExtension : fileNameWithExtension.substring(0, extensionIndex);
            // String outputFilePath = outputPath.resolve(fileName + ".json").toString();
            // String jsonResponse = JsonFormat.printer().includingDefaultValueFields().print(response);
            // Files.writeString(Paths.get(outputFilePath), jsonResponse);

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read or process receipt file: " + filePath, e);
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
                        receipt.getLineItems().add(lineItem);
                    }
                    break;
            }
        }

        if (receipt.getSubtotal() > 0 ) {
            double taxPercentage = (receipt.getTax() / receipt.getSubtotal()) * 100;
            receipt.setTaxPercentage(taxPercentage);
        }
        if (receipt.getSubtotal() > 0) {
            double tipPercentage = (receipt.getTip() / receipt.getSubtotal()) * 100;
            receipt.setTipPercentage(tipPercentage);
        }
        return receipt;
    }
}
