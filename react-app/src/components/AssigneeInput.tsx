import React, { useState, useRef, useEffect } from 'react';
import type { Friend } from '../types/friend';
import type { UserProfile } from '../types/userProfile';

/**
 * - ManualEntryStrategy: Simple text input (anonymous users)
 * - FriendSelectionStrategy: Autocomplete with friends (authenticated users)
 */

interface AssigneeInputProps {
  value: string;
  onChange: (value: string) => void;
  onBlur: () => void;
  placeholder?: string;
  friends?: Friend[]; // If provided, use friend selection strategy
  userProfile?: UserProfile | null; // User's own profile to include in suggestions
}

/**
 * AssigneeInput Component
 *
 * Factory Method Pattern: Creates appropriate input component based on friends availability
 * - If friends array is empty/undefined: returns ManualEntryInput
 * - If friends array has items: returns FriendSelectionInput
 */
export const AssigneeInput: React.FC<AssigneeInputProps> = ({
  value,
  onChange,
  onBlur,
  placeholder = "Assignees (comma-separated)",
  friends = [],
  userProfile = null,
}) => {
  const hasActiveFriendsOrProfile = (friends && friends.length > 0) || (userProfile && userProfile.displayName);

  if (!hasActiveFriendsOrProfile) {
    // Manual Entry (for anonymous or users without friends/profile)
    return <ManualEntryInput value={value} onChange={onChange} onBlur={onBlur} placeholder={placeholder} />;
  }

  // Friend Selection (for authenticated users with friends and/or profile)
  return <FriendSelectionInput value={value} onChange={onChange} onBlur={onBlur} friends={friends} userProfile={userProfile} placeholder={placeholder} />;
};

const ManualEntryInput: React.FC<{
  value: string;
  onChange: (value: string) => void;
  onBlur: () => void;
  placeholder: string;
}> = ({ value, onChange, onBlur, placeholder }) => {
  return (
    <input
      className="w-48 border p-1 rounded"
      type="text"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onBlur={onBlur}
      placeholder={placeholder}
    />
  );
};

const FriendSelectionInput: React.FC<{
  value: string;
  onChange: (value: string) => void;
  onBlur: () => void;
  friends: Friend[];
  userProfile?: UserProfile | null;
  placeholder: string;
}> = ({ value, onChange, onBlur, friends, userProfile, placeholder }) => {
  const [showSuggestions, setShowSuggestions] = useState(false);
  const [inputFocused, setInputFocused] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const blurTimeoutRef = useRef<number | null>(null);
  const isSelectingRef = useRef(false);

  const getLastTypedName = () => {
    const parts = value.split(',').map(s => s.trim());
    return parts[parts.length - 1] || '';
  };

  // Filter friends and user profile based on current input
  const getSuggestions = (): Array<{ displayName: string; contactEmail?: string; isOwner?: boolean }> => {
    const lastTyped = getLastTypedName().toLowerCase();

    // Build combined list: user profile first, then friends
    const suggestions: Array<{ displayName: string; contactEmail?: string; isOwner?: boolean }> = [];

    // Add user's own displayName if available
    if (userProfile && userProfile.displayName) {
      suggestions.push({
        displayName: userProfile.displayName,
        contactEmail: undefined,
        isOwner: true
      });
    }

    friends.forEach(friend => {
      suggestions.push({
        displayName: friend.displayName,
        contactEmail: friend.contactEmail,
        isOwner: false
      });
    });

    if (!lastTyped) return suggestions;

    return suggestions.filter(item => {
      const displayName = item.displayName.toLowerCase();
      return displayName.includes(lastTyped);
    });
  };

  // Handle friend/owner selection from dropdown
  const handleSelectSuggestion = (displayName: string) => {
    isSelectingRef.current = true;
    if (blurTimeoutRef.current) {
      clearTimeout(blurTimeoutRef.current);
      blurTimeoutRef.current = null;
    }

    const parts = value.split(',').map(s => s.trim()).filter(Boolean);
    const lastTyped = getLastTypedName();
    if (lastTyped && parts.length > 0) {
      parts.pop();
    }
    parts.push(displayName);

    // Don't add trailing comma if this is the only assignee
    const newValue = parts.length === 1 ? parts.join(', ') : parts.join(', ') + ', ';
    onChange(newValue);

    setShowSuggestions(false);
    setInputFocused(false);

    // Reset the selecting flag after a short delay
    setTimeout(() => {
      isSelectingRef.current = false;
    }, 50);

    // Keep focus on input for continued entry
    setTimeout(() => {
      inputRef.current?.focus();
      setInputFocused(true);
    }, 100);
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    onChange(e.target.value);
    setShowSuggestions(true);
  };

  const handleBlur = () => {
    if (blurTimeoutRef.current) {
      clearTimeout(blurTimeoutRef.current);
    }

    blurTimeoutRef.current = setTimeout(() => {
      if (isSelectingRef.current) {
        return;
      }

      setInputFocused(false);
      setShowSuggestions(false);
      onBlur();
      blurTimeoutRef.current = null;
    }, 200);
  };

  useEffect(() => {
    return () => {
      if (blurTimeoutRef.current) {
        clearTimeout(blurTimeoutRef.current);
      }
    };
  }, []);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        !dropdownRef.current.contains(event.target as Node) &&
        inputRef.current &&
        !inputRef.current.contains(event.target as Node)
      ) {
        setShowSuggestions(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const suggestions = getSuggestions();
  const showDropdown = showSuggestions && inputFocused && suggestions.length > 0;

  return (
    <div className="relative w-48">
      <input
        ref={inputRef}
        className="w-full border p-1 rounded"
        type="text"
        value={value}
        onChange={handleInputChange}
        onFocus={() => {
          setInputFocused(true);
          setShowSuggestions(true);
        }}
        onBlur={handleBlur}
        placeholder={placeholder}
      />

      {showDropdown && (
        <div
          ref={dropdownRef}
          className="absolute z-10 w-full mt-1 bg-white border border-gray-300 rounded-md shadow-lg max-h-48 overflow-y-auto"
        >
          {suggestions.map((item, idx) => (
            <button
              key={`${item.displayName}-${idx}`}
              type="button"
              className="w-full text-left px-3 py-2 hover:bg-blue-50 focus:bg-blue-50 focus:outline-none border-b border-gray-100 last:border-b-0"
              onMouseDown={(e) => {
                // Prevent blur event from firing before selection
                e.preventDefault();
                handleSelectSuggestion(item.displayName);
              }}
            >
              <div className="font-medium text-gray-900">
                {item.displayName}
                {item.isOwner && <span className="ml-2 text-xs text-blue-600">(You)</span>}
              </div>
              {item.contactEmail && (
                <div className="text-xs text-gray-500">
                  {item.contactEmail}
                </div>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};
