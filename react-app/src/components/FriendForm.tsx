import { useState } from 'react';
import type { Friend } from '../types/friend';

interface FriendFormProps {
  onSubmit: (friend: Friend) => void;
  onCancel?: () => void;
  initialData?: Friend;
  isEditing?: boolean;
}

export default function FriendForm({ onSubmit, onCancel, initialData, isEditing = false }: FriendFormProps) {
  const [formData, setFormData] = useState<Friend>({
    displayName: initialData?.displayName || '',
    venmoHandle: initialData?.venmoHandle || '',
    zellePhoneNumber: initialData?.zellePhoneNumber || '',
    paypalHandle: initialData?.paypalHandle || '',
    contactEmail: initialData?.contactEmail || '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    // Validation
    if (!formData.displayName.trim()) {
      alert('Display name is required');
      return;
    }

    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded-lg shadow">
      <h3 className="text-lg font-semibold text-gray-900">
        {isEditing ? 'Edit Friend' : 'Add New Friend'}
      </h3>

      <div>
        <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
          Display Name *
        </label>
        <input
          type="text"
          id="displayName"
          name="displayName"
          value={formData.displayName}
          onChange={handleChange}
          placeholder="John Doe"
          required
          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="space-y-3">
        <h4 className="text-sm font-medium text-gray-700">Contact & Payment Information</h4>

        <div>
          <label htmlFor="contactEmail" className="block text-sm text-gray-600 mb-1">
            Contact Email
          </label>
          <input
            type="email"
            id="contactEmail"
            name="contactEmail"
            value={formData.contactEmail}
            onChange={handleChange}
            placeholder="friend@example.com"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="venmoHandle" className="block text-sm text-gray-600 mb-1">
            Venmo Handle
          </label>
          <input
            type="text"
            id="venmoHandle"
            name="venmoHandle"
            value={formData.venmoHandle}
            onChange={handleChange}
            placeholder="@username"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="zellePhoneNumber" className="block text-sm text-gray-600 mb-1">
            Zelle Phone Number
          </label>
          <input
            type="tel"
            id="zellePhoneNumber"
            name="zellePhoneNumber"
            value={formData.zellePhoneNumber}
            onChange={handleChange}
            placeholder="(555) 123-4567"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label htmlFor="paypalHandle" className="block text-sm text-gray-600 mb-1">
            PayPal Handle
          </label>
          <input
            type="text"
            id="paypalHandle"
            name="paypalHandle"
            value={formData.paypalHandle}
            onChange={handleChange}
            placeholder="username@example.com"
            className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      <div className="flex gap-3 pt-2">
        <button
          type="submit"
          className="flex-1 bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition-colors"
        >
          {isEditing ? 'Update Friend' : 'Add Friend'}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="flex-1 bg-gray-200 text-gray-700 py-2 px-4 rounded-md hover:bg-gray-300 transition-colors"
          >
            Cancel
          </button>
        )}
      </div>
    </form>
  );
}
