import type { Friend } from '../types/friend';

interface FriendListProps {
  friends: Friend[];
  onEdit: (friend: Friend) => void;
  onDelete: (friendId: string) => void;
}

export default function FriendList({ friends, onEdit, onDelete }: FriendListProps) {
  if (friends.length === 0) {
    return (
      <div className="text-center py-12 bg-gray-50 rounded-lg">
        <p className="text-gray-500">No friends added yet. Add your first friend to get started!</p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {friends.map((friend) => (
        <div
          key={friend.id}
          className="bg-white p-6 rounded-lg shadow hover:shadow-md transition-shadow"
        >
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <h3 className="text-lg font-semibold text-gray-900">
                {friend.firstName} {friend.lastName}
              </h3>

              <div className="mt-3 space-y-2">
                {friend.venmoHandle && (
                  <div className="flex items-center text-sm">
                    <span className="font-medium text-gray-600 w-24">Venmo:</span>
                    <span className="text-gray-900">{friend.venmoHandle}</span>
                  </div>
                )}
                {friend.zellePhoneNumber && (
                  <div className="flex items-center text-sm">
                    <span className="font-medium text-gray-600 w-24">Zelle:</span>
                    <span className="text-gray-900">{friend.zellePhoneNumber}</span>
                  </div>
                )}
                {friend.paypalHandle && (
                  <div className="flex items-center text-sm">
                    <span className="font-medium text-gray-600 w-24">PayPal:</span>
                    <span className="text-gray-900">{friend.paypalHandle}</span>
                  </div>
                )}
                {!friend.venmoHandle && !friend.zellePhoneNumber && !friend.paypalHandle && (
                  <p className="text-sm text-gray-400 italic">No payment information added</p>
                )}
              </div>
            </div>

            <div className="flex gap-2 ml-4">
              <button
                onClick={() => onEdit(friend)}
                className="px-3 py-1.5 text-sm bg-blue-100 text-blue-700 rounded hover:bg-blue-200 transition-colors"
              >
                Edit
              </button>
              <button
                onClick={() => {
                  if (confirm(`Are you sure you want to delete ${friend.firstName} ${friend.lastName}?`)) {
                    onDelete(friend.id!);
                  }
                }}
                className="px-3 py-1.5 text-sm bg-red-100 text-red-700 rounded hover:bg-red-200 transition-colors"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}
