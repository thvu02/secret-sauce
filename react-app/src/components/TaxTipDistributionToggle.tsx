import React from 'react';

interface TaxTipDistributionToggleProps {
    value: 'proportional' | 'even';
    onChange: (value: 'proportional' | 'even') => void;
}

export const TaxTipDistributionToggle: React.FC<TaxTipDistributionToggleProps> = ({
    value,
    onChange,
}) => {
    return (
        <div className="space-y-2">
            <label className="block text-sm font-medium text-gray-700">
                Tax & Tip Distribution
            </label>
            <div className="flex gap-2">
                <button
                    type="button"
                    onClick={() => onChange('proportional')}
                    className={`flex-1 px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                        value === 'proportional'
                            ? 'bg-blue-600 text-white border-blue-600'
                            : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                >
                    <div className="font-semibold">Proportional</div>
                    <div className="text-xs mt-1 opacity-90">
                        Split by item costs
                    </div>
                </button>
                <button
                    type="button"
                    onClick={() => onChange('even')}
                    className={`flex-1 px-4 py-2 text-sm font-medium rounded-lg border transition-colors ${
                        value === 'even'
                            ? 'bg-blue-600 text-white border-blue-600'
                            : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
                    }`}
                >
                    <div className="font-semibold">Even</div>
                    <div className="text-xs mt-1 opacity-90">
                        Split evenly
                    </div>
                </button>
            </div>
            <p className="text-xs text-gray-500 mt-1">
                {value === 'proportional'
                    ? 'Tax and tip are split proportionally based on each person\'s share of items'
                    : 'Tax and tip are split evenly among all party members'}
            </p>
        </div>
    );
};
