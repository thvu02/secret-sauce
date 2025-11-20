import { useState, useEffect } from 'react';
import { friendApi } from '../services/api';
import type { Friend } from '../types/friend';
import FriendForm from '../components/FriendForm';
import FriendList from '../components/FriendList';
import { Navbar } from '../components';

export default function Profile() {
  const [friends, setFriends] = useState<Friend[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingFriend, setEditingFriend] = useState<Friend | null>(null);

  // Load friends on component mount
  useEffect(() => {
    loadFriends();
  }, []);

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

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <Navbar />
      <div className="max-w-4xl mx-auto px-4">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Friend Phonebook</h1>
          <p className="text-gray-600 mt-2">
            Manage your friends' contact and payment information
          </p>
        </div>

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
  );
}
