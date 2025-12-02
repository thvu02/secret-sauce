import { useState, useEffect } from 'react';
import { friendApi, userProfileApi } from '../services/api';
import type { Friend } from '../types/friend';
import type { UserProfile } from '../types/userProfile';
import FriendForm from '../components/FriendForm';
import FriendList from '../components/FriendList';
import { Navbar } from '../components';

export default function Profile() {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [userProfile, setUserProfile] = useState<UserProfile>({});
  const [profileSaved, setProfileSaved] = useState(false);
  const [loading, setLoading] = useState(true);
  const [profileLoading, setProfileLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingFriend, setEditingFriend] = useState<Friend | null>(null);

  // Load friends and user profile on component mount
  useEffect(() => {
    loadFriends();
    loadUserProfile();
  }, []);

  const loadUserProfile = async () => {
    try {
      setProfileLoading(true);
      setProfileError(null);
      const data = await userProfileApi.getProfile();
      setUserProfile(data);
    } catch (err: any) {
      // Profile might not exist yet - that's okay
      console.log('No profile found yet');
    } finally {
      setProfileLoading(false);
    }
  };

  const loadFriends = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await friendApi.getAllFriends();
      setFriends(data);
    } catch (err: any) {
      console.error('Error loading friends:', err);
      setError(err.message || 'Failed to load friends');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateFriend = async (friend: Friend) => {
    try {
      setError(null);
      const newFriend = await friendApi.createFriend(friend);
      setFriends((prev) => [...prev, newFriend]);
      setShowForm(false);
    } catch (err: any) {
      console.error('Error creating friend:', err);
      setError(err.message || 'Failed to create friend');
    }
  };

  const handleUpdateFriend = async (friend: Friend) => {
    if (!editingFriend?.id) return;

    try {
      setError(null);
      const updatedFriend = await friendApi.updateFriend(editingFriend.id, friend);
      setFriends((prev) =>
        prev.map((f) => (f.id === editingFriend.id ? updatedFriend : f))
      );
      setEditingFriend(null);
      setShowForm(false);
    } catch (err: any) {
      console.error('Error updating friend:', err);
      setError(err.message || 'Failed to update friend');
    }
  };

  const handleDeleteFriend = async (friendId: string) => {
    try {
      setError(null);
      await friendApi.deleteFriend(friendId);
      setFriends((prev) => prev.filter((f) => f.id !== friendId));
    } catch (err: any) {
      console.error('Error deleting friend:', err);
      setError(err.message || 'Failed to delete friend');
    }
  };

  const handleEdit = (friend: Friend) => {
    setEditingFriend(friend);
    setShowForm(true);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingFriend(null);
  };

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setProfileError(null);
      setProfileSaved(false);

      // Try to update first, if it fails, create
      try {
        const updated = await userProfileApi.updateProfile(userProfile);
        setUserProfile(updated);
      } catch (updateErr) {
        // If update fails, try create
        const created = await userProfileApi.createProfile(userProfile);
        setUserProfile(created);
      }

      setProfileSaved(true);
      setTimeout(() => setProfileSaved(false), 3000);
    } catch (err: any) {
      console.error('Error saving profile:', err);
      setProfileError(err.message || 'Failed to save profile');
    }
  };

  const handleProfileChange = (field: keyof UserProfile, value: string) => {
    setUserProfile(prev => ({ ...prev, [field]: value }));
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Profile</h1>
          <p className="text-gray-600 mt-2">
            Manage your payment information and friend phonebook
          </p>
        </div>

        {/* User Profile Section */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-8">
          <h2 className="text-2xl font-semibold text-gray-800 mb-4">Your Payment Information</h2>
          <p className="text-gray-600 mb-6">
            This information will be included when sending payment reminders to friends
          </p>

          {profileError && (
            <div className="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-700">{profileError}</p>
            </div>
          )}

          {profileSaved && (
            <div className="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
              <p className="text-green-700">Profile saved successfully!</p>
            </div>
          )}

          {profileLoading ? (
            <div className="text-center py-8">
              <p className="text-gray-500">Loading profile...</p>
            </div>
          ) : (
            <form onSubmit={handleProfileSubmit}>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="md:col-span-2">
                  <label htmlFor="displayName" className="block text-sm font-medium text-gray-700 mb-1">
                    Display Name *
                  </label>
                  <input
                    type="text"
                    id="displayName"
                    value={userProfile.displayName || ''}
                    onChange={(e) => handleProfileChange('displayName', e.target.value)}
                    placeholder="John Doe"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    required
                  />
                  <p className="text-xs text-gray-500 mt-1">This name will appear on payment reminders</p>
                </div>

                <div>
                  <label htmlFor="venmoHandle" className="block text-sm font-medium text-gray-700 mb-1">
                    Venmo Handle
                  </label>
                  <input
                    type="text"
                    id="venmoHandle"
                    value={userProfile.venmoHandle || ''}
                    onChange={(e) => handleProfileChange('venmoHandle', e.target.value)}
                    placeholder="@username"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div>
                  <label htmlFor="paypalHandle" className="block text-sm font-medium text-gray-700 mb-1">
                    PayPal Handle
                  </label>
                  <input
                    type="text"
                    id="paypalHandle"
                    value={userProfile.paypalHandle || ''}
                    onChange={(e) => handleProfileChange('paypalHandle', e.target.value)}
                    placeholder="email@example.com"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>

                <div className="md:col-span-2">
                  <label htmlFor="zellePhoneNumber" className="block text-sm font-medium text-gray-700 mb-1">
                    Zelle Phone Number
                  </label>
                  <input
                    type="tel"
                    id="zellePhoneNumber"
                    value={userProfile.zellePhoneNumber || ''}
                    onChange={(e) => handleProfileChange('zellePhoneNumber', e.target.value)}
                    placeholder="+1234567890"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  />
                </div>
              </div>

              <div className="mt-6">
                <button
                  type="submit"
                  className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition-colors font-medium"
                >
                  Save Payment Information
                </button>
              </div>
            </form>
          )}
        </div>

        {/* Friends Section */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <h2 className="text-2xl font-semibold text-gray-800 mb-4">Friend Phonebook</h2>
          <p className="text-gray-600 mb-6">
            Manage your friends' contact and payment information
          </p>

          {error && (
            <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
              <p className="text-red-700">{error}</p>
            </div>
          )}

          {!showForm && (
            <div className="mb-6">
              <button
                onClick={() => setShowForm(true)}
                className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition-colors font-medium"
              >
                + Add New Friend
              </button>
            </div>
          )}

          {showForm && (
            <div className="mb-8">
              <FriendForm
                onSubmit={editingFriend ? handleUpdateFriend : handleCreateFriend}
                onCancel={handleCancel}
                initialData={editingFriend || undefined}
                isEditing={!!editingFriend}
              />
            </div>
          )}

          {loading ? (
            <div className="text-center py-12">
              <p className="text-gray-500">Loading friends...</p>
            </div>
          ) : (
            <FriendList
              friends={friends}
              onEdit={handleEdit}
              onDelete={handleDeleteFriend}
            />
          )}
        </div>
      </div>
    </div>
  );
}
