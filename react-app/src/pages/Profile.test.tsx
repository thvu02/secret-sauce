import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import Profile from './Profile';
import { friendApi } from '../services/api';
import type { Friend } from '../types/friend';

vi.mock('../services/api', () => ({
    friendApi: {
        getAllFriends: vi.fn(),
        createFriend: vi.fn(),
        updateFriend: vi.fn(),
        deleteFriend: vi.fn(),
    },
}));

vi.mock('../components', () => ({
    Navbar: () => <div>Navbar</div>,
}));

const renderProfile = () => {
    return render(
        <BrowserRouter>
            <Profile />
        </BrowserRouter>
    );
};

describe('Profile', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should display page title', async () => {
            (friendApi.getAllFriends as any).mockResolvedValue([]);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Friend Phonebook')).toBeInTheDocument();
                expect(screen.getByText(/manage your friends' contact and payment information/i))
                    .toBeInTheDocument();
            });
        });

        it('should show loading state initially', () => {
            (friendApi.getAllFriends as any).mockImplementation(
                () => new Promise(() => {})
            );

            renderProfile();

            expect(screen.getByText(/loading friends/i)).toBeInTheDocument();
        });
    });

    describe('when there are no friends', () => {
        it('should display empty state after loading', async () => {
            (friendApi.getAllFriends as any).mockResolvedValue([]);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText(/no friends added yet/i)).toBeInTheDocument();
            });
        });

        it('should show add friend button', async () => {
            (friendApi.getAllFriends as any).mockResolvedValue([]);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });
        });
    });

    describe('when there are friends', () => {
        const mockFriends: Friend[] = [
            {
                id: '1',
                displayName: 'Alice Smith',
                contactEmail: 'alice@example.com',
                venmoHandle: '@alice',
                zellePhoneNumber: '555-1111',
                paypalHandle: 'alice@paypal.com',
            },
            {
                id: '2',
                displayName: 'Bob Jones',
                contactEmail: '',
                venmoHandle: '',
                zellePhoneNumber: '',
                paypalHandle: '',
            },
        ];

        it('should display all friends', async () => {
            (friendApi.getAllFriends as any).mockResolvedValue(mockFriends);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Alice Smith')).toBeInTheDocument();
                expect(screen.getByText('Bob Jones')).toBeInTheDocument();
            });
        });
    });

    describe('adding a friend', () => {
        it('should show form when add button is clicked', async () => {
            const user = userEvent.setup();
            (friendApi.getAllFriends as any).mockResolvedValue([]);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });

            const addButton = screen.getByRole('button', { name: /add new friend/i });
            await user.click(addButton);

            expect(screen.getByText('Add New Friend')).toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /add new friend/i }))
                .not.toBeInTheDocument();
        });

        it('should create friend on form submission', async () => {
            const user = userEvent.setup();
            const newFriend: Friend = {
                id: '3',
                displayName: 'Charlie Brown',
                contactEmail: '',
                venmoHandle: '@charlie',
                zellePhoneNumber: '',
                paypalHandle: '',
            };

            (friendApi.getAllFriends as any).mockResolvedValue([]);
            (friendApi.createFriend as any).mockResolvedValue(newFriend);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /add new friend/i }));

            // Get all display name inputs and use the last one (friend form)
            const displayNameInputs = screen.getAllByLabelText(/display name/i);
            await user.type(displayNameInputs[displayNameInputs.length - 1], 'Charlie Brown');
            const venmoHandleInputs = screen.getAllByLabelText(/venmo handle/i);
            await user.type(venmoHandleInputs[venmoHandleInputs.length - 1], '@charlie');

            await user.click(screen.getByRole('button', { name: /add friend/i }));

            await waitFor(() => {
                expect(friendApi.createFriend).toHaveBeenCalledWith(
                    expect.objectContaining({
                        displayName: 'Charlie Brown',
                        venmoHandle: '@charlie',
                    })
                );
            });
        });

        it('should hide form after successful creation', async () => {
            const user = userEvent.setup();
            const newFriend: Friend = {
                id: '3',
                displayName: 'Charlie Brown',
                contactEmail: '',
                venmoHandle: '',
                zellePhoneNumber: '',
                paypalHandle: '',
            };

            (friendApi.getAllFriends as any).mockResolvedValue([]);
            (friendApi.createFriend as any).mockResolvedValue(newFriend);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /add new friend/i }));

            // Get all display name inputs and use the last one (friend form)
            const displayNameInputs = screen.getAllByLabelText(/display name/i);
            await user.type(displayNameInputs[displayNameInputs.length - 1], 'Charlie Brown');
            await user.click(screen.getByRole('button', { name: /add friend/i }));

            await waitFor(() => {
                expect(screen.queryByText('Add New Friend')).not.toBeInTheDocument();
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });
        });
    });

    describe('editing a friend', () => {
        const mockFriends: Friend[] = [
            {
                id: '1',
                displayName: 'Alice Smith',
                contactEmail: '',
                venmoHandle: '@alice',
                zellePhoneNumber: '',
                paypalHandle: '',
            },
        ];

        it('should show edit form when edit button is clicked', async () => {
            const user = userEvent.setup();
            (friendApi.getAllFriends as any).mockResolvedValue(mockFriends);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Alice Smith')).toBeInTheDocument();
            });

            const editButton = screen.getByRole('button', { name: /edit/i });
            await user.click(editButton);

            expect(screen.getByText('Edit Friend')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Alice Smith')).toBeInTheDocument();
        });

        it('should update friend on form submission', async () => {
            const user = userEvent.setup();
            const updatedFriend: Friend = {
                id: '1',
                displayName: 'Alice Johnson',
                contactEmail: '',
                venmoHandle: '@alice',
                zellePhoneNumber: '',
                paypalHandle: '',
            };

            (friendApi.getAllFriends as any).mockResolvedValue(mockFriends);
            (friendApi.updateFriend as any).mockResolvedValue(updatedFriend);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Alice Smith')).toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /edit/i }));

            // Get all display name inputs and use the last one (friend form)
            const displayNameInputs = screen.getAllByLabelText(/display name/i);
            const displayNameInput = displayNameInputs[displayNameInputs.length - 1];
            await user.clear(displayNameInput);
            await user.type(displayNameInput, 'Alice Johnson');

            await user.click(screen.getByRole('button', { name: /update friend/i }));

            await waitFor(() => {
                expect(friendApi.updateFriend).toHaveBeenCalledWith(
                    '1',
                    expect.objectContaining({
                        displayName: 'Alice Johnson',
                    })
                );
            });
        });
    });

    describe('deleting a friend', () => {
        const mockFriends: Friend[] = [
            {
                id: '1',
                displayName: 'Alice Smith',
                contactEmail: '',
                venmoHandle: '',
                zellePhoneNumber: '',
                paypalHandle: '',
            },
        ];

        it('should delete friend when confirmed', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

            (friendApi.getAllFriends as any).mockResolvedValue(mockFriends);
            (friendApi.deleteFriend as any).mockResolvedValue(undefined);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Alice Smith')).toBeInTheDocument();
            });

            const deleteButton = screen.getByRole('button', { name: /delete/i });
            await user.click(deleteButton);

            await waitFor(() => {
                expect(friendApi.deleteFriend).toHaveBeenCalledWith('1');
            });

            confirmSpy.mockRestore();
        });
    });

    describe('error handling', () => {
        it('should display error when loading fails', async () => {
            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
            (friendApi.getAllFriends as any).mockRejectedValue(
                new Error('Failed to load friends')
            );

            renderProfile();

            await waitFor(() => {
                expect(screen.getByText('Failed to load friends')).toBeInTheDocument();
            });

            consoleErrorSpy.mockRestore();
        });

        it('should display error when creation fails', async () => {
            const user = userEvent.setup();
            const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

            (friendApi.getAllFriends as any).mockResolvedValue([]);
            (friendApi.createFriend as any).mockRejectedValue(
                new Error('Failed to create friend')
            );

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /add new friend/i }));

            // Get all display name inputs and use the last one (friend form)
            const displayNameInputs = screen.getAllByLabelText(/display name/i);
            await user.type(displayNameInputs[displayNameInputs.length - 1], 'Charlie Brown');
            await user.click(screen.getByRole('button', { name: /add friend/i }));

            await waitFor(() => {
                expect(screen.getByText('Failed to create friend')).toBeInTheDocument();
            });

            consoleErrorSpy.mockRestore();
        });
    });

    describe('cancel functionality', () => {
        it('should hide form when cancel is clicked', async () => {
            const user = userEvent.setup();
            (friendApi.getAllFriends as any).mockResolvedValue([]);

            renderProfile();

            await waitFor(() => {
                expect(screen.getByRole('button', { name: /add new friend/i }))
                    .toBeInTheDocument();
            });

            await user.click(screen.getByRole('button', { name: /add new friend/i }));
            expect(screen.getByText('Add New Friend')).toBeInTheDocument();

            await user.click(screen.getByRole('button', { name: /cancel/i }));

            expect(screen.queryByText('Add New Friend')).not.toBeInTheDocument();
            expect(screen.getByRole('button', { name: /add new friend/i }))
                .toBeInTheDocument();
        });
    });
});
