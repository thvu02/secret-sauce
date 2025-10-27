import React, { useState, useEffect } from 'react';
import Navbar from '../components/Navbar.tsx';

type LineItem = {
    name?: string;
    price?: number;
    quantity?: number;
    assignees?: string[];
    numAssignees?: number;
    assigneesText?: string;
};

type Receipt = {
    uid?: string;
    vendor?: string;
    receiptDate?: string;
    currency?: string;
    subtotal?: number;
    tax?: number;
    taxPercentage?: number;
    tip?: number;
    tipPercentage?: number;
    total?: number;
    lineItems?: LineItem[];
};

const BACKEND_BASE = 'http://localhost:8080/api';

const Home: React.FC = () => {
    const [file, setFile] = useState<File | null>(null);
    const [previewUrl, setPreviewUrl] = useState<string | null>(null);
    const [receipt, setReceipt] = useState<Receipt | null>(null);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState<string | null>(null);
    const [testMode, setTestMode] = useState<boolean>(true);
    const [testAccounts, setTestAccounts] = useState<string[]>([]);
    const [venmoRequests, setVenmoRequests] = useState<Array<{
        assignee: string, 
        amount: number, 
        venmoApp: string, 
        venmoWeb: string,
        paymentResult?: {
            success: boolean,
            transactionId?: string,
            status?: string,
            message?: string,
            error?: string,
            testMode?: boolean
        }
    }>>([]);

    useEffect(() => {
        document.title = 'Receipt OCR - Home';
        // Load payment config
        fetch(`${BACKEND_BASE}/payment/config`)
            .then(res => res.json())
            .then(data => {
                setTestMode(data.testMode);
                if (data.testAccounts) {
                    setTestAccounts(data.testAccounts);
                }
            })
            .catch(console.error);
    }, []);

        function handleFileChange(e: React.ChangeEvent<HTMLInputElement>) {
        const f = e.target.files && e.target.files[0];
        if (!f) return;
        setFile(f);
        const reader = new FileReader();
        reader.onload = () => setPreviewUrl(reader.result as string);
        reader.readAsDataURL(f);
    }

    async function uploadFile() {
        if (!file) return setMessage('Please select a file first');
        setLoading(true);
        setMessage(null);
        try {
            const fd = new FormData();
            fd.append('file', file);
            const res = await fetch(`${BACKEND_BASE}/ocr/upload`, {
                method: 'POST',
                body: fd,
            });
            if (!res.ok) {
                const err = await res.json();
                throw new Error(err.error || 'Upload failed');
            }
            const json = await res.json();
            setReceipt(json as Receipt);
            setMessage('Parsed receipt ready for review');
        } catch (err: any) {
            console.error(err);
            setMessage(err.message || 'Upload failed');
        } finally {
            setLoading(false);
        }
    }

    function recalculateTotals(receipt: Receipt): Receipt {
        const subtotal = receipt.subtotal || 0;
        const tax = receipt.tax || 0;
        const tip = receipt.tip || 0;
        const total = subtotal + tax + tip;

        const taxPercentage = subtotal > 0 ? (tax / subtotal) * 100 : 0;
        const tipPercentage = subtotal > 0 ? (tip / subtotal) * 100 : 0;

        return {
            ...receipt,
            total,
            taxPercentage,
            tipPercentage
        };
    }

    function updateField<K extends keyof Receipt>(key: K, value: Receipt[K]) {
        setReceipt((r) => {
            if (!r) return r;
            const updated = { ...r, [key]: value };
            
            if (['subtotal', 'tax', 'tip'].includes(key)) {
                return recalculateTotals(updated);
            }
            
            return updated;
        });
    }

    function updateLineItem(index: number, key: keyof LineItem, value: any) {
        setReceipt((r) => {
            if (!r) return r;
            const items = (r.lineItems || []).map((it, i) => {
                if (i !== index) return it;
                if (key === 'assignees') {
                    return { ...it, assigneesText: String(value) } as LineItem;
                }

                if (key === 'price' || key === 'quantity' || key === 'numAssignees') {
                    return { ...it, [key]: Number(value) } as LineItem;
                }

                return { ...it, [key]: value };
            });

            const subtotal = items.reduce((sum, item) => sum + ((item.price || 0) * (item.quantity || 1)), 0);
            const updated = { ...r, lineItems: items, subtotal } as Receipt;

            return recalculateTotals(updated);
        });
    }

    function addLineItem() {
        setReceipt((r) => ({ ...r!, lineItems: [...(r?.lineItems || []), { name: '', price: 0, quantity: 1, assignees: [], numAssignees: 0 }] } as Receipt));
    }

    function removeLineItem(i: number) {
        setReceipt((r) => ({ ...r!, lineItems: (r?.lineItems || []).filter((_, idx) => idx !== i) } as Receipt));
    }

    async function saveReceipt() {
        if (!receipt) return setMessage('Nothing to save');
        setLoading(true);
        setMessage(null);
        try {
            const normalized = { ...receipt } as Receipt;
            normalized.lineItems = (normalized.lineItems || []).map(li => {
                const text = li.assigneesText ?? (li.assignees || []).join(', ');
                const arr = String(text).split(',').map(s => s.trim()).filter(Boolean);
                return { ...li, assignees: arr, numAssignees: arr.length } as LineItem;
            });

            const res = await fetch(`${BACKEND_BASE}/receipts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(normalized),
            });
            const json = await res.json();
            if (!res.ok) {
                console.error('Server error details:', json);
                throw new Error(json.error || `Save failed (${res.status})`);
            }
            setMessage('Saved to database');
        } catch (err: any) {
            console.error('Save error:', err);
            const errorMsg = err.message || 'Failed to save receipt';
            setMessage(`Error: ${errorMsg}`);
        } finally {
            setLoading(false);
        }
    }

    async function requestVenmo() {
        if (!receipt) return setMessage('Nothing to request');
        setLoading(true);
        setMessage(null);
        try {
            const normalized = { ...receipt } as Receipt;
            normalized.lineItems = (normalized.lineItems || []).map(li => {
                const text = li.assigneesText ?? (li.assignees || []).join(', ');
                const arr = String(text).split(',').map(s => s.trim()).filter(Boolean);
                return { ...li, assignees: arr, numAssignees: arr.length } as LineItem;
            });

            const res = await fetch(`${BACKEND_BASE}/venmo/requests`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...normalized, testMode }),
            });
            const json = await res.json();
            if (!res.ok) {
                console.error('Venmo request error:', json);
                throw new Error(json.error || `Venmo compute failed (${res.status})`);
            }

            setVenmoRequests(json as any);
            setMessage('Venmo requests computed');
        } catch (err: any) {
            console.error('Venmo request error:', err);
            setMessage(err.message || 'Failed to compute Venmo requests');
        } finally {
            setLoading(false);
        }
    }

    return (
        <div>
            <Navbar />
            <div className="mx-auto min-h-screen max-w-screen-xl px-6 py-12 font-sans md:px-6 md:py-12 lg:py-0">
                <div className="lg:justify-between lg:gap-4">
                    <main className="pt-24 lg:py-24">
                        <div className="flex items-center justify-between mb-6">
                            <h2 className="text-section">Upload Receipt Image</h2>
                            <div className="flex items-center gap-4">
                                <label className="flex items-center gap-2">
                                    <input
                                        type="checkbox"
                                        checked={testMode}
                                        onChange={(e) => setTestMode(e.target.checked)}
                                        className="w-4 h-4"
                                    />
                                    <span className="text-sm font-medium">Test Mode</span>
                                </label>
                                {testMode && testAccounts.length > 0 && (
                                    <div className="text-sm text-gray-600">
                                        Test accounts: {testAccounts.join(', ')}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div className="mb-4">
                            <input type="file" accept="image/*" onChange={handleFileChange} disabled={loading} />
                            <button className="ml-2 px-3 py-1 bg-blue-600 text-white rounded" onClick={uploadFile} disabled={loading || !file}>
                                {loading ? 'Processing receipt with OCR...' : 'Upload & Parse'}
                            </button>
                            {loading && <div className="mt-2 text-sm text-gray-600">This may take a few moments while we process your receipt...</div>}
                        </div>

                        {previewUrl && (
                            <div className="mb-4">
                                <h3 className="font-medium">Preview</h3>
                                <img src={previewUrl} alt="preview" style={{ maxWidth: '320px', maxHeight: '480px' }} />
                            </div>
                        )}

                        {message && <div className="mb-4 text-sm text-gray-700">{message}</div>}

                        {receipt && (
                            <section className="bg-white shadow rounded p-4">
                                <h3 className="text-lg font-semibold mb-2">Parsed Receipt</h3>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <div>
                                        <label className="block text-sm">Vendor</label>
                                        <input className="w-full border p-2" value={receipt.vendor || ''} onChange={(e) => updateField('vendor', e.target.value)} />
                                    </div>
                                    <div>
                                        <label className="block text-sm">Date</label>
                                        <input className="w-full border p-2" value={receipt.receiptDate || ''} onChange={(e) => updateField('receiptDate', e.target.value)} />
                                    </div>
                                    <div>
                                        <label className="block text-sm">Subtotal</label>
                                        <input className="w-full border p-2" type="number" step="0.01" value={receipt.subtotal ?? 0} onChange={(e) => updateField('subtotal', parseFloat(e.target.value || '0'))} />
                                    </div>
                                    <div>
                                        <label className="block text-sm">Tax</label>
                                        <input className="w-full border p-2" type="number" step="0.01" value={receipt.tax ?? 0} onChange={(e) => updateField('tax', parseFloat(e.target.value || '0'))} />
                                    </div>
                                    <div>
                                        <label className="block text-sm">Tip</label>
                                        <input className="w-full border p-2" type="number" step="0.01" value={receipt.tip ?? 0} onChange={(e) => updateField('tip', parseFloat(e.target.value || '0'))} />
                                    </div>
                                    <div>
                                        <label className="block text-sm">Total</label>
                                        <input className="w-full border p-2" type="number" step="0.01" value={receipt.total ?? 0} onChange={(e) => updateField('total', parseFloat(e.target.value || '0'))} />
                                    </div>
                                </div>

                                <div className="mt-4">
                                    <h4 className="font-medium">Line Items</h4>
                                    {(receipt.lineItems || []).map((li, i) => (
                                        <div key={i} className="flex gap-2 items-center my-2">
                                            <input className="flex-1 border p-1" value={li.name || ''} onChange={(e) => updateLineItem(i, 'name', e.target.value)} placeholder="name" />
                                            <input className="w-28 border p-1" type="number" step="0.01" value={li.price ?? 0} onChange={(e) => updateLineItem(i, 'price', parseFloat(e.target.value || '0'))} placeholder="price" />
                                            <input className="w-20 border p-1" type="number" value={li.quantity ?? 1} onChange={(e) => updateLineItem(i, 'quantity', parseInt(e.target.value || '1'))} placeholder="qty" />
                                            <input
                                                className="w-48 border p-1"
                                                type="text"
                                                value={li.assigneesText ?? (li.assignees || []).join(', ')}
                                                onChange={(e) => updateLineItem(i, 'assignees', e.target.value)}
                                                onBlur={() => {
                                                    const text = (receipt?.lineItems?.[i]?.assigneesText ?? (receipt?.lineItems?.[i]?.assignees || []).join(', '));
                                                    const arr = String(text).split(',').map(s => s.trim()).filter(Boolean);
                                                    updateLineItem(i, 'assignees', arr);
                                                    updateLineItem(i, 'numAssignees', arr.length);
                                                }}
                                                placeholder="assignees (comma-separated)"
                                            />
                                            <div className="text-sm text-gray-600">{(li.numAssignees ?? (li.assignees || []).length) + ' assigned'}</div>
                                            <button className="px-2 py-1 bg-red-500 text-white rounded" onClick={() => removeLineItem(i)}>Remove</button>
                                        </div>
                                    ))}
                                    <button className="mt-2 px-3 py-1 bg-green-600 text-white rounded" onClick={addLineItem}>Add Line Item</button>
                                </div>

                                <div className="mt-4 flex gap-2">
                                    <button className="px-4 py-2 bg-blue-600 text-white rounded" onClick={saveReceipt} disabled={loading}>Save to Database</button>
                                    <button className="px-4 py-2 bg-green-600 text-white rounded" onClick={requestVenmo} disabled={loading}>Request Venmo</button>
                                </div>
                            </section>
                        )}

                        {venmoRequests && venmoRequests.length > 0 && (
                            <section className="bg-white shadow rounded p-4 mt-4">
                                <div className="flex items-center justify-between mb-2">
                                    <h3 className="text-lg font-semibold">Venmo Requests</h3>
                                    {testMode && (
                                        <div className="text-sm px-2 py-1 bg-yellow-100 text-yellow-800 rounded">
                                            Test Mode Active - Using Sandbox Environment
                                        </div>
                                    )}
                                </div>
                                <div className="flex flex-col gap-2">
                                    {venmoRequests.map((r, idx) => (
                                        <div key={idx} className="flex items-center gap-4">
                                            <div className="flex-1">
                                                <div>{r.assignee} — ${r.amount.toFixed(2)}</div>
                                                {r.paymentResult && (
                                                    <div className={`text-sm mt-1 ${r.paymentResult.success ? 'text-green-600' : 'text-red-600'}`}>
                                                        {r.paymentResult.success ? (
                                                            <>Payment requested (ID: {r.paymentResult.transactionId})</>
                                                        ) : (
                                                            <>Failed: {r.paymentResult.message || r.paymentResult.error}</>
                                                        )}
                                                    </div>
                                                )}
                                            </div>
                                            <button 
                                                className={`px-3 py-1 text-white rounded ${r.paymentResult?.success ? 'bg-gray-400' : 'bg-blue-500'}`} 
                                                onClick={() => {
                                                    if (testMode) {
                                                        window.alert('Test Mode: Would launch Venmo app in production environment.');
                                                        return;
                                                    }
                                                    window.open(r.venmoApp);
                                                }}
                                                disabled={r.paymentResult?.success}
                                            >
                                                {r.paymentResult?.success ? 'Requested' : 'Open Venmo App'}
                                            </button>
                                            {!r.paymentResult?.success && (
                                                <a className="px-3 py-1 bg-gray-200 text-gray-800 rounded" href={r.venmoWeb} target="_blank" rel="noreferrer">
                                                    Open Web
                                                </a>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </section>
                        )}
                    </main>
                </div>
            </div>
        </div>
    );
};

export default Home;
