import React, { useState, useEffect } from 'react';
import { Navbar, FileUploadSection, ReceiptFields, LineItemsSection } from '../components';
import { useFileUpload } from '../hooks/useFileUpload';
import { useReceipt } from '../hooks/useReceipt';
import { receiptApi, ApiError } from '../services/api';
import { normalizeReceiptLineItems } from '../utils/receiptCalculations';
import type { Receipt } from '../types';

export const Home: React.FC = () => {
    const { file, previewUrl, handleFileChange } = useFileUpload();
    const {
        receipt,
        setReceipt,
        updateField,
        updateLineItem,
        addLineItem,
        removeLineItem,
    } = useReceipt();

    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);

    useEffect(() => {
        document.title = 'Receipt OCR - Home';

        // Initialize with empty receipt on page load
        const emptyReceipt: Receipt = {
            vendor: '',
            receiptDate: '',
            currency: 'USD',
            subtotal: 0,
            tax: 0,
            taxPercentage: 0,
            tip: 0,
            tipPercentage: 0,
            total: 0,
            taxTipDistribution: 'proportional',
            lineItems: [
                {
                    name: '',
                    price: 0,
                    quantity: 1,
                    assignees: [],
                    splitMode: 'equal',
                    assigneePercentages: {}
                }
            ],
        };
        setReceipt(emptyReceipt);
    }, [setReceipt]);

    const handleUpload = async () => {
        if (!file) {
            setMessage('Please select a file first');
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            const parsedReceipt = await receiptApi.uploadReceipt(file);
            setReceipt(parsedReceipt);
            setMessage('Receipt data populated from image - review and edit as needed');
        } catch (error) {
            const errorMessage = error instanceof ApiError
                ? error.message
                : 'Upload failed';
            console.error('Upload error:', error);
            setMessage(`Error: ${errorMessage}`);
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        if (!receipt) {
            setMessage('Nothing to save');
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            const normalized = normalizeReceiptLineItems(receipt);
            await receiptApi.saveReceipt(normalized);
            setMessage('Saved to database successfully');
        } catch (error) {
            const errorMessage = error instanceof ApiError
                ? error.message
                : 'Failed to save receipt';
            console.error('Save error:', error);
            setMessage(`Error: ${errorMessage}`);
        } finally {
            setLoading(false);
        }
    };

    const handleGenerateReport = async () => {
        if (!receipt) {
            setMessage('Nothing to generate');
            return;
        }

        setLoading(true);
        setMessage(null);

        try {
            const normalized = normalizeReceiptLineItems(receipt);
            await receiptApi.generateReport(normalized);
            setMessage('PDF report generated and downloaded successfully');
        } catch (error) {
            const errorMessage = error instanceof ApiError
                ? error.message
                : 'Failed to generate report';
            console.error('Generate report error:', error);
            setMessage(`Error: ${errorMessage}`);
        } finally {
            setLoading(false);
        }
    };

    const handleAssigneeBlur = (index: number) => {
        if (!receipt) return;

        const lineItem = receipt.lineItems?.[index];
        if (!lineItem) return;

        const text = lineItem.assigneesText ?? (lineItem.assignees || []).join(', ');
        const assignees = String(text)
            .split(',')
            .map(s => s.trim())
            .filter(Boolean);

        updateLineItem(index, 'assignees', assignees);
    };

    return (
        <div>
            <Navbar />

            <div className="mx-auto min-h-screen max-w-screen-xl px-6 py-12 font-sans md:px-6 md:py-12 lg:py-0">
                <div className="lg:justify-between lg:gap-4">
                    <main className="pt-24 lg:py-24">
                        <FileUploadSection
                            file={file}
                            previewUrl={previewUrl}
                            loading={loading}
                            onFileChange={handleFileChange}
                            onUpload={handleUpload}
                        />

                        {message && (
                            <div className={`mb-4 p-3 rounded ${
                                message.startsWith('Error')
                                    ? 'bg-red-100 text-red-700'
                                    : 'bg-green-100 text-green-700'
                            }`}>
                                {message}
                            </div>
                        )}

                        {receipt && (
                            <section className="bg-white shadow rounded p-4">
                                <h3 className="text-lg font-semibold mb-4">Receipt Details</h3>

                                <ReceiptFields
                                    receipt={receipt}
                                    onUpdate={updateField}
                                />

                                <LineItemsSection
                                    lineItems={receipt.lineItems || []}
                                    onUpdate={updateLineItem}
                                    onAdd={addLineItem}
                                    onRemove={removeLineItem}
                                    onAssigneeBlur={handleAssigneeBlur}
                                />

                                <div className="mt-4 flex gap-2">
                                    <button
                                        className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:bg-gray-400"
                                        onClick={handleSave}
                                        disabled={loading}
                                    >
                                        {loading ? 'Saving...' : 'Save to Database'}
                                    </button>
                                    <button
                                        className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:bg-gray-400"
                                        onClick={handleGenerateReport}
                                        disabled={loading}
                                    >
                                        {loading ? 'Generating...' : 'Generate Report'}
                                    </button>
                                </div>
                            </section>
                        )}
                    </main>
                </div>
            </div>
        </div>
    );
};
