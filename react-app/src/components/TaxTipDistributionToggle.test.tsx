import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { TaxTipDistributionToggle } from './TaxTipDistributionToggle';

describe('TaxTipDistributionToggle', () => {
    it('should render the label', () => {
        const onChange = vi.fn();
        render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

        expect(screen.getByText('Tax & Tip Distribution')).toBeInTheDocument();
    });

    it('should render both buttons', () => {
        const onChange = vi.fn();
        render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

        expect(screen.getByRole('button', { name: /proportional/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /even/i })).toBeInTheDocument();
    });

    describe('when proportional is selected', () => {
        it('should highlight proportional button', () => {
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

            const proportionalButton = screen.getByRole('button', { name: /proportional/i });
            const evenButton = screen.getByRole('button', { name: /even/i });

            expect(proportionalButton).toHaveClass('bg-blue-600', 'text-white');
            expect(evenButton).toHaveClass('bg-white', 'text-gray-700');
        });

        it('should display proportional description', () => {
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

            expect(
                screen.getByText(/Tax and tip are split proportionally/i)
            ).toBeInTheDocument();
        });
    });

    describe('when even is selected', () => {
        it('should highlight even button', () => {
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="even" onChange={onChange} />);

            const proportionalButton = screen.getByRole('button', { name: /proportional/i });
            const evenButton = screen.getByRole('button', { name: /even/i });

            expect(evenButton).toHaveClass('bg-blue-600', 'text-white');
            expect(proportionalButton).toHaveClass('bg-white', 'text-gray-700');
        });

        it('should display even description', () => {
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="even" onChange={onChange} />);

            expect(
                screen.getByText(/Tax and tip are split evenly/i)
            ).toBeInTheDocument();
        });
    });

    describe('interactions', () => {
        it('should call onChange with "proportional" when proportional button is clicked', async () => {
            const user = userEvent.setup();
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="even" onChange={onChange} />);

            const proportionalButton = screen.getByRole('button', { name: /proportional/i });
            await user.click(proportionalButton);

            expect(onChange).toHaveBeenCalledTimes(1);
            expect(onChange).toHaveBeenCalledWith('proportional');
        });

        it('should call onChange with "even" when even button is clicked', async () => {
            const user = userEvent.setup();
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

            const evenButton = screen.getByRole('button', { name: /even/i });
            await user.click(evenButton);

            expect(onChange).toHaveBeenCalledTimes(1);
            expect(onChange).toHaveBeenCalledWith('even');
        });

        it('should allow clicking the same button multiple times', async () => {
            const user = userEvent.setup();
            const onChange = vi.fn();
            render(<TaxTipDistributionToggle value="proportional" onChange={onChange} />);

            const proportionalButton = screen.getByRole('button', { name: /proportional/i });
            await user.click(proportionalButton);
            await user.click(proportionalButton);

            expect(onChange).toHaveBeenCalledTimes(2);
            expect(onChange).toHaveBeenNthCalledWith(1, 'proportional');
            expect(onChange).toHaveBeenNthCalledWith(2, 'proportional');
        });
    });
});
