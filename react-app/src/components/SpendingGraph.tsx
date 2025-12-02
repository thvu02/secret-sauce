import React, { useMemo, useState } from 'react';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';
import type { Receipt, LineItem } from '../types';
import type { UserProfile } from '../types/userProfile';

interface SpendingGraphProps {
    receipts: Receipt[];
    userProfile: UserProfile | null;
}

interface MonthlyData {
    month: string;
    spending: number;
}

export const SpendingGraph: React.FC<SpendingGraphProps> = ({ receipts, userProfile }) => {
    // Get user's display name for matching
    const userFullName = useMemo(() => {
        if (!userProfile) return null;
        return userProfile.displayName || null;
    }, [userProfile]);

    // Calculate user's share of a line item
    const calculateUserShare = (lineItem: LineItem): number => {
        if (!userFullName || !lineItem.assignees || lineItem.assignees.length === 0) {
            return 0;
        }

        // Check if user is assigned to this item
        if (!lineItem.assignees.includes(userFullName)) {
            return 0;
        }

        const itemTotal = (lineItem.price || 0) * (lineItem.quantity || 1);

        // Calculate share based on split mode
        if (lineItem.splitMode === 'percentage' && lineItem.assigneePercentages?.[userFullName]) {
            return itemTotal * (lineItem.assigneePercentages[userFullName] / 100);
        } else {
            // Equal split
            return itemTotal / lineItem.assignees.length;
        }
    };

    // Calculate user's share of tax and tip
    const calculateUserTaxTip = (receipt: Receipt, userSubtotal: number): { tax: number; tip: number } => {
        const taxTipMode = receipt.taxTipDistribution || 'proportional';

        if (taxTipMode === 'even') {
            // Count total unique assignees in receipt
            const uniqueAssignees = new Set<string>();
            receipt.lineItems?.forEach(item => {
                item.assignees?.forEach(assignee => uniqueAssignees.add(assignee));
            });
            const totalAssignees = uniqueAssignees.size;

            return {
                tax: totalAssignees > 0 ? (receipt.tax || 0) / totalAssignees : 0,
                tip: totalAssignees > 0 ? (receipt.tip || 0) / totalAssignees : 0,
            };
        } else {
            // Proportional distribution
            const proportion = (receipt.subtotal || 0) > 0 ? userSubtotal / (receipt.subtotal || 1) : 0;
            return {
                tax: proportion * (receipt.tax || 0),
                tip: proportion * (receipt.tip || 0),
            };
        }
    };
    // Get list of unique years from receipts
    const availableYears = useMemo(() => {
        const years = new Set<number>();
        receipts.forEach(receipt => {
            if (receipt.receiptDate) {
                const date = new Date(receipt.receiptDate);
                if (!isNaN(date.getTime())) {
                    years.add(date.getFullYear());
                }
            }
        });
        return Array.from(years).sort((a, b) => b - a); // Most recent first
    }, [receipts]);

    // Default to current year or most recent year with data
    const currentYear = new Date().getFullYear();
    const defaultYear = availableYears.includes(currentYear) ? currentYear : availableYears[0] || currentYear;
    const [selectedYear, setSelectedYear] = useState<number>(defaultYear);

    // Calculate monthly spending for selected year (only user's share)
    const monthlyData = useMemo((): MonthlyData[] => {
        // Initialize all 12 months with 0
        const monthNames = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
        const monthlySpending = Array(12).fill(0);

        if (!userFullName) {
            // If no user profile, return empty data
            return monthNames.map(name => ({ month: name, spending: 0 }));
        }

        // Aggregate spending by month (only user's share)
        receipts.forEach(receipt => {
            if (!receipt.receiptDate || !receipt.lineItems) return;

            const date = new Date(receipt.receiptDate);
            if (isNaN(date.getTime())) return;

            const year = date.getFullYear();
            const month = date.getMonth(); // 0-11

            if (year === selectedYear) {
                // Calculate user's share of line items
                let userSubtotal = 0;
                receipt.lineItems.forEach(lineItem => {
                    userSubtotal += calculateUserShare(lineItem);
                });

                // Calculate user's share of tax and tip
                const { tax, tip } = calculateUserTaxTip(receipt, userSubtotal);

                // Add user's total share to monthly spending
                const userTotal = userSubtotal + tax + tip;
                monthlySpending[month] += userTotal;
            }
        });

        // Convert to chart data format
        return monthNames.map((name, index) => ({
            month: name,
            spending: parseFloat(monthlySpending[index].toFixed(2)),
        }));
    }, [receipts, selectedYear, userFullName]);

    // Calculate total annual spending
    const totalAnnualSpending = useMemo(() => {
        return monthlyData.reduce((sum, data) => sum + data.spending, 0);
    }, [monthlyData]);

    // Calculate average monthly spending
    const averageMonthlySpending = useMemo(() => {
        const nonZeroMonths = monthlyData.filter(data => data.spending > 0).length;
        return nonZeroMonths > 0 ? totalAnnualSpending / nonZeroMonths : 0;
    }, [monthlyData, totalAnnualSpending]);

    // Find highest spending month
    const highestSpendingMonth = useMemo(() => {
        return monthlyData.reduce((max, data) =>
            data.spending > max.spending ? data : max
        , monthlyData[0]);
    }, [monthlyData]);

    return (
        <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex items-center justify-between mb-6">
                <div>
                    <h2 className="text-2xl font-semibold text-gray-800">Annual Spending Overview</h2>
                    <p className="text-gray-600 text-sm mt-1">Track your monthly expenses throughout the year</p>
                </div>

                {availableYears.length > 0 && (
                    <div>
                        <label htmlFor="year-select" className="block text-sm font-medium text-gray-700 mb-1">
                            Select Year
                        </label>
                        <select
                            id="year-select"
                            value={selectedYear}
                            onChange={(e) => setSelectedYear(parseInt(e.target.value))}
                            className="px-4 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        >
                            {availableYears.map(year => (
                                <option key={year} value={year}>
                                    {year}
                                </option>
                            ))}
                        </select>
                    </div>
                )}
            </div>

            {/* Statistics Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
                <div className="bg-blue-50 rounded-lg p-4">
                    <p className="text-sm text-blue-600 font-medium">Total Annual Spending</p>
                    <p className="text-2xl font-bold text-blue-900 mt-1">
                        ${totalAnnualSpending.toFixed(2)}
                    </p>
                </div>

                <div className="bg-green-50 rounded-lg p-4">
                    <p className="text-sm text-green-600 font-medium">Average Monthly Spending</p>
                    <p className="text-2xl font-bold text-green-900 mt-1">
                        ${averageMonthlySpending.toFixed(2)}
                    </p>
                </div>

                <div className="bg-purple-50 rounded-lg p-4">
                    <p className="text-sm text-purple-600 font-medium">Highest Spending Month</p>
                    <p className="text-2xl font-bold text-purple-900 mt-1">
                        {highestSpendingMonth.spending > 0 ? highestSpendingMonth.month : 'N/A'}
                    </p>
                    {highestSpendingMonth.spending > 0 && (
                        <p className="text-sm text-purple-700 mt-1">
                            ${highestSpendingMonth.spending.toFixed(2)}
                        </p>
                    )}
                </div>
            </div>

            {/* Area Chart */}
            {totalAnnualSpending > 0 ? (
                <div className="mt-6">
                    <ResponsiveContainer width="100%" height={400}>
                        <AreaChart
                            data={monthlyData}
                            margin={{ top: 5, right: 30, left: 20, bottom: 5 }}
                        >
                            <defs>
                                <linearGradient id="colorSpending" x1="0" y1="0" x2="0" y2="1">
                                    <stop offset="5%" stopColor="#2563eb" stopOpacity={0.8}/>
                                    <stop offset="95%" stopColor="#2563eb" stopOpacity={0.1}/>
                                </linearGradient>
                            </defs>
                            <CartesianGrid strokeDasharray="3 3" />
                            <XAxis
                                dataKey="month"
                                label={{ value: 'Month', position: 'insideBottom', offset: -5 }}
                            />
                            <YAxis
                                label={{ value: 'Spending ($)', angle: -90, position: 'insideLeft' }}
                            />
                            <Tooltip
                                formatter={(value: number) => [`$${value.toFixed(2)}`, 'Spending']}
                                contentStyle={{ backgroundColor: '#fff', border: '1px solid #ccc', borderRadius: '8px' }}
                            />
                            <Legend />
                            <Area
                                type="monotone"
                                dataKey="spending"
                                stroke="#2563eb"
                                strokeWidth={2}
                                fill="url(#colorSpending)"
                                name="Monthly Spending"
                            />
                        </AreaChart>
                    </ResponsiveContainer>
                </div>
            ) : (
                <div className="text-center py-12 bg-gray-50 rounded-lg">
                    <p className="text-gray-500">No spending data available for {selectedYear}</p>
                    <p className="text-sm text-gray-400 mt-2">
                        {!userFullName ? 'Please complete your profile to track your spending' : 'Upload receipts and assign yourself to see your spending trends'}
                    </p>
                </div>
            )}
        </div>
    );
};
