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
                firstName: 'Alice',
                lastName: 'Smith',
                venmoHandle: '@alice',
                zellePhoneNumber: '555-1111',
                paypalHandle: 'alice@example.com',
            },
            {
                id: '2',
                firstName: 'Bob',
                lastName: 'Jones',
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
                firstName: 'Charlie',
                lastName: 'Brown',
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

            await user.type(screen.getByLabelText(/first name/i), 'Charlie');
            await user.type(screen.getByLabelText(/last name/i), 'Brown');
            await user.type(screen.getByLabelText(/venmo handle/i), '@charlie');

            await user.click(screen.getByRole('button', { name: /add friend/i }));

            await waitFor(() => {
                expect(friendApi.createFriend).toHaveBeenCalledWith(
                    expect.objectContaining({
                        firstName: 'Charlie',
                        lastName: 'Brown',
                        venmoHandle: '@charlie',
                    })
                );
            });
        });

        it('should hide form after successful creation', async () => {
            const user = userEvent.setup();
            const newFriend: Friend = {
                id: '3',
                firstName: 'Charlie',
                lastName: 'Brown',
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
            await user.type(screen.getByLabelText(/first name/i), 'Charlie');
            await user.type(screen.getByLabelText(/last name/i), 'Brown');
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
                firstName: 'Alice',
                lastName: 'Smith',
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
            expect(screen.getByDisplayValue('Alice')).toBeInTheDocument();
        });

        it('should update friend on form submission', async () => {
            const user = userEvent.setup();
            const updatedFriend: Friend = {
                id: '1',
                firstName: 'Alice',
                lastName: 'Johnson',
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

            const lastNameInput = screen.getByLabelText(/last name/i);
            await user.clear(lastNameInput);
            await user.type(lastNameInput, 'Johnson');

            await user.click(screen.getByRole('button', { name: /update friend/i }));

            await waitFor(() => {
                expect(friendApi.updateFriend).toHaveBeenCalledWith(
                    '1',
                    expect.objectContaining({
                        lastName: 'Johnson',
                    })
                );
            });
        });
    });

    describe('deleting a friend', () => {
        const mockFriends: Friend[] = [
            {
                id: '1',
                firstName: 'Alice',
                lastName: 'Smith',
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
            await user.type(screen.getByLabelText(/first name/i), 'Charlie');
            await user.type(screen.getByLabelText(/last name/i), 'Brown');
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
