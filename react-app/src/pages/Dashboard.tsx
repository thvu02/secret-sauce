import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { dashboardApi, receiptApi, friendApi, userProfileApi, ApiError } from '../services/api';
import { useAuth } from '../hooks/useAuth';
import { PaymentStatusTracker } from '../components/PaymentStatusTracker';
import { SpendingGraph } from '../components/SpendingGraph';
import type { Receipt } from '../types';
import type { Friend } from '../types/friend';
import type { UserProfile } from '../types/userProfile';
import { Navbar } from '../components';

export const Dashboard: React.FC = () => {
    const { user } = useAuth();
    const [receipts, setReceipts] = useState<Receipt[]>([]);
    const [friends, setFriends] = useState<Friend[]>([]);
    const [userProfile, setUserProfile] = useState<UserProfile | null>(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedReceipt, setSelectedReceipt] = useState<Receipt | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const [receiptsData, friendsData, profileData] = await Promise.all([
                    dashboardApi.getUserReceipts(),
                    friendApi.getAllFriends().catch(() => []), // Gracefully handle missing friends
                    userProfileApi.getProfile().catch(() => null), // Gracefully handle missing profile
                ]);
                setReceipts(receiptsData);
                setFriends(friendsData);
                setUserProfile(profileData);
            } catch (err) {
                if (err instanceof ApiError) {
                    setError(err.message);
                } else {
                    setError('Failed to load receipts');
                }
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, []);

    const handleGeneratePdf = async (receipt: Receipt) => {
        try {
            await receiptApi.generateReport(receipt);
        } catch (err) {
            console.error('Failed to generate PDF:', err);
            alert('Failed to generate PDF report');
        }
    };

    const handlePaymentUpdate = async () => {
        try {
            // Refresh the selected receipt to get updated payment status
            if (selectedReceipt && selectedReceipt.uid) {
                const updatedReceipt = await dashboardApi.getReceipt(selectedReceipt.uid);
                setSelectedReceipt(updatedReceipt);

                // Also refresh the receipts list to update the payment status badge
                const updatedReceipts = await dashboardApi.getUserReceipts();
                setReceipts(updatedReceipts);
            }
        } catch (err) {
            console.error('Failed to refresh receipt:', err);
        }
    };

    const handleDeleteReceipt = async (receiptId: string, receiptVendor: string) => {
        if (!window.confirm(`Are you sure you want to delete the receipt from ${receiptVendor || 'Unknown Vendor'}?`)) {
            return;
        }

        try {
            await dashboardApi.deleteReceipt(receiptId);

            // Close modal if the deleted receipt was being viewed
            if (selectedReceipt && selectedReceipt.uid === receiptId) {
                setSelectedReceipt(null);
            }

            // Refresh the receipts list
            const updatedReceipts = await dashboardApi.getUserReceipts();
            setReceipts(updatedReceipts);
        } catch (err) {
            console.error('Failed to delete receipt:', err);
            if (err instanceof ApiError) {
                alert(`Failed to delete receipt: ${err.message}`);
            } else {
                alert('Failed to delete receipt');
            }
        }
    };

    const getPaymentStatusBadge = (status: string) => {
        const classes = {
            pending: 'bg-yellow-100 text-yellow-800',
            partial: 'bg-blue-100 text-blue-800',
            complete: 'bg-green-100 text-green-800',
        };

        const labels = {
            pending: 'Pending',
            partial: 'Partial',
            complete: 'Complete',
        };

        return (
            <span
                className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
                    classes[status as keyof typeof classes] || classes.pending
                }`}
            >
                {labels[status as keyof typeof labels] || status}
            </span>
        );
    };

    if (isLoading) {
        return (
            <div className="flex justify-center items-center min-h-screen">
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            </div>
        );
    }

    return (
        <div className="min-h-screen bg-gray-50 py-8">
            <Navbar />
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="mb-8">
                    <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
                    <p className="mt-2 text-sm text-gray-600">
                        Welcome back, {user?.email}! Here are your saved receipts.
                    </p>
                </div>

                {error && (
                    <div className="mb-4 rounded-md bg-red-50 p-4">
                        <div className="text-sm text-red-800">{error}</div>
                    </div>
                )}

                {/* Spending Analytics Graph */}
                {receipts.length > 0 && (
                    <div className="mb-8">
                        <SpendingGraph receipts={receipts} userProfile={userProfile} />
                    </div>
                )}

                {/* Receipts List Section */}
                <div className="mb-4">
                    <h2 className="text-2xl font-semibold text-gray-800">Your Receipts</h2>
                </div>

                {receipts.length === 0 ? (
                    <div className="text-center py-12">
                        <svg
                            className="mx-auto h-12 w-12 text-gray-400"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                            />
                        </svg>
                        <h3 className="mt-2 text-sm font-medium text-gray-900">No receipts</h3>
                        <p className="mt-1 text-sm text-gray-500">
                            Get started by uploading a receipt.
                        </p>
                        <div className="mt-6">
                            <Link
                                to="/"
                                className="inline-flex items-center px-4 py-2 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700"
                            >
                                Upload Receipt
                            </Link>
                        </div>
                    </div>
                ) : (
                    <div className="bg-white shadow overflow-hidden sm:rounded-md">
                        <ul className="divide-y divide-gray-200">
                            {receipts.map((receipt) => (
                                <li key={receipt.uid}>
                                    <div className="px-4 py-4 sm:px-6 hover:bg-gray-50">
                                        <div className="flex items-center justify-between">
                                            <div className="flex-1 min-w-0">
                                                <div className="flex items-center justify-between">
                                                    <p className="text-sm font-medium text-blue-600 truncate">
                                                        {receipt.vendor || 'Unknown Vendor'}
                                                    </p>
                                                    {receipt.paymentStatus &&
                                                        getPaymentStatusBadge(receipt.paymentStatus)}
                                                </div>
                                                <div className="mt-2 flex items-center text-sm text-gray-500">
                                                    <span>
                                                        {receipt.receiptDate || 'No date'} • Total:{' '}
                                                        {receipt.total?.toFixed(2) || '0.00'} {receipt.currency || 'USD'}
                                                    </span>
                                                </div>
                                                {receipt.lineItems && receipt.lineItems.length > 0 && (
                                                    <div className="mt-1 text-xs text-gray-500">
                                                        {receipt.lineItems.length} item(s)
                                                    </div>
                                                )}
                                            </div>
                                            <div className="ml-5 flex-shrink-0 flex space-x-2">
                                                <button
                                                    onClick={() => setSelectedReceipt(receipt)}
                                                    className="inline-flex items-center px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                                                >
                                                    View Details
                                                </button>
                                                <button
                                                    onClick={() => handleGeneratePdf(receipt)}
                                                    className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
                                                >
                                                    Download PDF
                                                </button>
                                                <button
                                                    onClick={() => receipt.uid && handleDeleteReceipt(receipt.uid, receipt.vendor || 'Unknown Vendor')}
                                                    className="inline-flex items-center px-3 py-2 border border-transparent text-sm leading-4 font-medium rounded-md text-white bg-red-600 hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500"
                                                >
                                                    Delete
                                                </button>
                                            </div>
                                        </div>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}

                {/* Receipt Details Modal */}
                {selectedReceipt && (
                    <div className="fixed z-50 inset-0 overflow-y-auto" aria-labelledby="modal-title" role="dialog" aria-modal="true">
                        <div className="flex items-center justify-center min-h-screen p-4">
                            {/* Background overlay */}
                            <div
                                className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
                                onClick={() => setSelectedReceipt(null)}
                                aria-hidden="true"
                            ></div>

                            {/* Modal panel */}
                            <div className="relative z-10 bg-white rounded-lg px-6 py-6 shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                                <div>
                                    <div className="flex items-center justify-between mb-4">
                                        <h3 className="text-lg leading-6 font-medium text-gray-900">
                                            Receipt Details
                                        </h3>
                                        {selectedReceipt.paymentStatus &&
                                            getPaymentStatusBadge(selectedReceipt.paymentStatus)}
                                    </div>

                                    <div className="space-y-3">
                                        <div>
                                            <span className="font-semibold">Vendor:</span>{' '}
                                            {selectedReceipt.vendor || 'N/A'}
                                        </div>
                                        <div>
                                            <span className="font-semibold">Date:</span>{' '}
                                            {selectedReceipt.receiptDate || 'N/A'}
                                        </div>
                                        <div>
                                            <span className="font-semibold">Subtotal:</span> $
                                            {selectedReceipt.subtotal?.toFixed(2) || '0.00'}
                                        </div>
                                        <div>
                                            <span className="font-semibold">Tax:</span> $
                                            {selectedReceipt.tax?.toFixed(2) || '0.00'}
                                        </div>
                                        <div>
                                            <span className="font-semibold">Tip:</span> $
                                            {selectedReceipt.tip?.toFixed(2) || '0.00'}
                                        </div>
                                        <div className="font-bold">
                                            <span className="font-semibold">Total:</span> $
                                            {selectedReceipt.total?.toFixed(2) || '0.00'}
                                        </div>

                                        {selectedReceipt.lineItems &&
                                            selectedReceipt.lineItems.length > 0 && (
                                                <div className="mt-4">
                                                    <h4 className="font-semibold mb-2">Line Items:</h4>
                                                    <ul className="space-y-2">
                                                        {selectedReceipt.lineItems.map((item, idx) => (
                                                            <li
                                                                key={idx}
                                                                className="text-sm border-l-2 border-gray-200 pl-3"
                                                            >
                                                                <div>
                                                                    {item.name} - $
                                                                    {((item.price || 0) * (item.quantity || 1)).toFixed(2)}
                                                                </div>
                                                                {item.assignees &&
                                                                    item.assignees.length > 0 && (
                                                                        <div className="text-xs text-gray-500">
                                                                            Assignees:{' '}
                                                                            {item.assignees.join(', ')}
                                                                        </div>
                                                                    )}
                                                            </li>
                                                        ))}
                                                    </ul>
                                                </div>
                                            )}
                                    </div>
                                </div>

                                {/* Payment Status Tracker */}
                                {selectedReceipt.uid && selectedReceipt.lineItems && selectedReceipt.lineItems.length > 0 && (
                                    <div className="border-t border-gray-200 pt-4 mt-4">
                                        <PaymentStatusTracker
                                            receiptId={selectedReceipt.uid}
                                            lineItems={selectedReceipt.lineItems}
                                            assigneePaymentStatus={selectedReceipt.assigneePaymentStatus}
                                            subtotal={selectedReceipt.subtotal}
                                            tax={selectedReceipt.tax}
                                            tip={selectedReceipt.tip}
                                            taxTipDistribution={selectedReceipt.taxTipDistribution}
                                            onUpdate={handlePaymentUpdate}
                                            friends={friends}
                                        />
                                    </div>
                                )}

                                <div className="mt-5 sm:mt-6 flex gap-3">
                                    <button
                                        type="button"
                                        onClick={() => selectedReceipt.uid && handleDeleteReceipt(selectedReceipt.uid, selectedReceipt.vendor || 'Unknown Vendor')}
                                        className="flex-1 inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:text-sm"
                                    >
                                        Delete Receipt
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setSelectedReceipt(null)}
                                        className="flex-1 inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 sm:text-sm"
                                    >
                                        Close
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
