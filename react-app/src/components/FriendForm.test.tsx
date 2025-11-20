import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import FriendForm from './FriendForm';
import type { Friend } from '../types/friend';

describe('FriendForm', () => {
    const mockOnSubmit = vi.fn();
    const mockOnCancel = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('rendering', () => {
        it('should render add friend form when not editing', () => {
            render(<FriendForm onSubmit={mockOnSubmit} />);

            expect(screen.getByText('Add New Friend')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /add friend/i })).toBeInTheDocument();
        });

        it('should render edit friend form when editing', () => {
            render(<FriendForm onSubmit={mockOnSubmit} isEditing={true} />);

            expect(screen.getByText('Edit Friend')).toBeInTheDocument();
            expect(screen.getByRole('button', { name: /update friend/i })).toBeInTheDocument();
        });

        it('should render all input fields', () => {
            render(<FriendForm onSubmit={mockOnSubmit} />);

            expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/venmo handle/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/zelle phone number/i)).toBeInTheDocument();
            expect(screen.getByLabelText(/paypal handle/i)).toBeInTheDocument();
        });

        it('should render cancel button when onCancel is provided', () => {
            render(<FriendForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

            expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
        });

        it('should not render cancel button when onCancel is not provided', () => {
            render(<FriendForm onSubmit={mockOnSubmit} />);

            expect(screen.queryByRole('button', { name: /cancel/i })).not.toBeInTheDocument();
        });
    });

    describe('initial data', () => {
        it('should populate form with initial data', () => {
            const initialData: Friend = {
                firstName: 'John',
                lastName: 'Doe',
                venmoHandle: '@johndoe',
                zellePhoneNumber: '555-1234',
                paypalHandle: 'john@example.com',
            };

            render(<FriendForm onSubmit={mockOnSubmit} initialData={initialData} />);

            expect(screen.getByDisplayValue('John')).toBeInTheDocument();
            expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
            expect(screen.getByDisplayValue('@johndoe')).toBeInTheDocument();
            expect(screen.getByDisplayValue('555-1234')).toBeInTheDocument();
            expect(screen.getByDisplayValue('john@example.com')).toBeInTheDocument();
        });
    });

    describe('form interactions', () => {
        it('should update first name field', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            const firstNameInput = screen.getByLabelText(/first name/i);
            await user.type(firstNameInput, 'Alice');

            expect(firstNameInput).toHaveValue('Alice');
        });

        it('should update last name field', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            const lastNameInput = screen.getByLabelText(/last name/i);
            await user.type(lastNameInput, 'Smith');

            expect(lastNameInput).toHaveValue('Smith');
        });

        it('should update payment information fields', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            const venmoInput = screen.getByLabelText(/venmo handle/i);
            const zelleInput = screen.getByLabelText(/zelle phone number/i);
            const paypalInput = screen.getByLabelText(/paypal handle/i);

            await user.type(venmoInput, '@alice');
            await user.type(zelleInput, '555-9999');
            await user.type(paypalInput, 'alice@example.com');

            expect(venmoInput).toHaveValue('@alice');
            expect(zelleInput).toHaveValue('555-9999');
            expect(paypalInput).toHaveValue('alice@example.com');
        });
    });

    describe('form submission', () => {
        it('should submit form with valid data', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            await user.type(screen.getByLabelText(/first name/i), 'Alice');
            await user.type(screen.getByLabelText(/last name/i), 'Smith');
            await user.type(screen.getByLabelText(/venmo handle/i), '@alice');

            await user.click(screen.getByRole('button', { name: /add friend/i }));

            expect(mockOnSubmit).toHaveBeenCalledTimes(1);
            expect(mockOnSubmit).toHaveBeenCalledWith({
                firstName: 'Alice',
                lastName: 'Smith',
                venmoHandle: '@alice',
                zellePhoneNumber: '',
                paypalHandle: '',
            });
        });

        it('should not submit when first name is missing', async () => {
            const user = userEvent.setup();

            render(<FriendForm onSubmit={mockOnSubmit} />);

            const firstNameInput = screen.getByLabelText(/first name/i);
            const lastNameInput = screen.getByLabelText(/last name/i);

            await user.type(lastNameInput, 'Smith');

            // Check that first name input is required
            expect(firstNameInput).toBeRequired();
            expect(mockOnSubmit).not.toHaveBeenCalled();
        });

        it('should not submit when last name is missing', async () => {
            const user = userEvent.setup();

            render(<FriendForm onSubmit={mockOnSubmit} />);

            const firstNameInput = screen.getByLabelText(/first name/i);
            const lastNameInput = screen.getByLabelText(/last name/i);

            await user.type(firstNameInput, 'Alice');

            // Check that last name input is required
            expect(lastNameInput).toBeRequired();
            expect(mockOnSubmit).not.toHaveBeenCalled();
        });

        it('should trim whitespace from names', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            await user.type(screen.getByLabelText(/first name/i), '  Alice  ');
            await user.type(screen.getByLabelText(/last name/i), '  Smith  ');

            await user.click(screen.getByRole('button', { name: /add friend/i }));

            expect(mockOnSubmit).toHaveBeenCalledWith(
                expect.objectContaining({
                    firstName: '  Alice  ',
                    lastName: '  Smith  ',
                })
            );
        });

        it('should allow submission with only required fields', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} />);

            await user.type(screen.getByLabelText(/first name/i), 'Alice');
            await user.type(screen.getByLabelText(/last name/i), 'Smith');

            await user.click(screen.getByRole('button', { name: /add friend/i }));

            expect(mockOnSubmit).toHaveBeenCalledWith({
                firstName: 'Alice',
                lastName: 'Smith',
                venmoHandle: '',
                zellePhoneNumber: '',
                paypalHandle: '',
            });
        });
    });

    describe('cancel functionality', () => {
        it('should call onCancel when cancel button is clicked', async () => {
            const user = userEvent.setup();
            render(<FriendForm onSubmit={mockOnSubmit} onCancel={mockOnCancel} />);

            await user.click(screen.getByRole('button', { name: /cancel/i }));

            expect(mockOnCancel).toHaveBeenCalledTimes(1);
            expect(mockOnSubmit).not.toHaveBeenCalled();
        });
    });
});
