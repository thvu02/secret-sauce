import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FriendList from './FriendList';
import type { Friend } from '../types/friend';

describe('FriendList', () => {
    const mockOnEdit = vi.fn();
    const mockOnDelete = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('when there are no friends', () => {
        it('should display empty state message', () => {
            render(<FriendList friends={[]} onEdit={mockOnEdit} onDelete={mockOnDelete} />);

            expect(
                screen.getByText(/No friends added yet/i)
            ).toBeInTheDocument();
        });

        it('should not display any friend cards', () => {
            render(<FriendList friends={[]} onEdit={mockOnEdit} onDelete={mockOnDelete} />);

            expect(screen.queryByRole('button', { name: /edit/i })).not.toBeInTheDocument();
            expect(screen.queryByRole('button', { name: /delete/i })).not.toBeInTheDocument();
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

        it('should render all friends', () => {
            render(
                <FriendList friends={mockFriends} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(screen.getByText('Alice Smith')).toBeInTheDocument();
            expect(screen.getByText('Bob Jones')).toBeInTheDocument();
        });

        it('should display payment information when available', () => {
            render(
                <FriendList friends={mockFriends} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(screen.getByText('@alice')).toBeInTheDocument();
            expect(screen.getByText('555-1111')).toBeInTheDocument();
            expect(screen.getByText('alice@paypal.com')).toBeInTheDocument();
        });

        it('should display no payment information message when not available', () => {
            render(
                <FriendList friends={mockFriends} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(
                screen.getByText(/No payment information added/i)
            ).toBeInTheDocument();
        });

        it('should render edit button for each friend', () => {
            render(
                <FriendList friends={mockFriends} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            const editButtons = screen.getAllByRole('button', { name: /edit/i });
            expect(editButtons).toHaveLength(2);
        });

        it('should render delete button for each friend', () => {
            render(
                <FriendList friends={mockFriends} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            const deleteButtons = screen.getAllByRole('button', { name: /delete/i });
            expect(deleteButtons).toHaveLength(2);
        });
    });

    describe('edit functionality', () => {
        const mockFriend: Friend = {
            id: '1',
            displayName: 'Alice Smith',
            contactEmail: 'alice@example.com',
            venmoHandle: '@alice',
            zellePhoneNumber: '555-1111',
            paypalHandle: 'alice@paypal.com',
        };

        it('should call onEdit with friend data when edit is clicked', async () => {
            const user = userEvent.setup();
            render(
                <FriendList
                    friends={[mockFriend]}
                    onEdit={mockOnEdit}
                    onDelete={mockOnDelete}
                />
            );

            const editButton = screen.getByRole('button', { name: /edit/i });
            await user.click(editButton);

            expect(mockOnEdit).toHaveBeenCalledTimes(1);
            expect(mockOnEdit).toHaveBeenCalledWith(mockFriend);
        });
    });

    describe('delete functionality', () => {
        const mockFriend: Friend = {
            id: '123',
            displayName: 'Alice Smith',
            contactEmail: 'alice@example.com',
            venmoHandle: '@alice',
            zellePhoneNumber: '555-1111',
            paypalHandle: 'alice@paypal.com',
        };

        it('should show confirmation dialog when delete is clicked', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

            render(
                <FriendList
                    friends={[mockFriend]}
                    onEdit={mockOnEdit}
                    onDelete={mockOnDelete}
                />
            );

            const deleteButton = screen.getByRole('button', { name: /delete/i });
            await user.click(deleteButton);

            expect(confirmSpy).toHaveBeenCalledWith(
                'Are you sure you want to delete Alice Smith?'
            );

            confirmSpy.mockRestore();
        });

        it('should call onDelete with friend id when confirmed', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true);

            render(
                <FriendList
                    friends={[mockFriend]}
                    onEdit={mockOnEdit}
                    onDelete={mockOnDelete}
                />
            );

            const deleteButton = screen.getByRole('button', { name: /delete/i });
            await user.click(deleteButton);

            expect(mockOnDelete).toHaveBeenCalledTimes(1);
            expect(mockOnDelete).toHaveBeenCalledWith('123');

            confirmSpy.mockRestore();
        });

        it('should not call onDelete when cancelled', async () => {
            const user = userEvent.setup();
            const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(false);

            render(
                <FriendList
                    friends={[mockFriend]}
                    onEdit={mockOnEdit}
                    onDelete={mockOnDelete}
                />
            );

            const deleteButton = screen.getByRole('button', { name: /delete/i });
            await user.click(deleteButton);

            expect(mockOnDelete).not.toHaveBeenCalled();

            confirmSpy.mockRestore();
        });
    });

    describe('payment information display', () => {
        it('should show only venmo when only venmo is provided', () => {
            const friend: Friend = {
                id: '1',
                displayName: 'Alice Smith',
                contactEmail: '',
                venmoHandle: '@alice',
                zellePhoneNumber: '',
                paypalHandle: '',
            };

            render(
                <FriendList friends={[friend]} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(screen.getByText('Venmo:')).toBeInTheDocument();
            expect(screen.getByText('@alice')).toBeInTheDocument();
            expect(screen.queryByText('Zelle:')).not.toBeInTheDocument();
            expect(screen.queryByText('PayPal:')).not.toBeInTheDocument();
        });

        it('should show only zelle when only zelle is provided', () => {
            const friend: Friend = {
                id: '1',
                displayName: 'Bob Jones',
                contactEmail: '',
                venmoHandle: '',
                zellePhoneNumber: '555-9999',
                paypalHandle: '',
            };

            render(
                <FriendList friends={[friend]} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(screen.getByText('Zelle:')).toBeInTheDocument();
            expect(screen.getByText('555-9999')).toBeInTheDocument();
            expect(screen.queryByText('Venmo:')).not.toBeInTheDocument();
            expect(screen.queryByText('PayPal:')).not.toBeInTheDocument();
        });

        it('should show only paypal when only paypal is provided', () => {
            const friend: Friend = {
                id: '1',
                displayName: 'Charlie Brown',
                contactEmail: '',
                venmoHandle: '',
                zellePhoneNumber: '',
                paypalHandle: 'charlie@example.com',
            };

            render(
                <FriendList friends={[friend]} onEdit={mockOnEdit} onDelete={mockOnDelete} />
            );

            expect(screen.getByText('PayPal:')).toBeInTheDocument();
            expect(screen.getByText('charlie@example.com')).toBeInTheDocument();
            expect(screen.queryByText('Venmo:')).not.toBeInTheDocument();
            expect(screen.queryByText('Zelle:')).not.toBeInTheDocument();
        });
    });
});
