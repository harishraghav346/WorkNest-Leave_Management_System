import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import AddLeave from '../components/AddLeave';
import ViewLeave from '../components/ViewLeave';
import UpdateLeave from '../components/UpdateLeave';
import * as leaveService from '../services/leaveService';
import "@testing-library/jest-dom"
jest.mock('../services/leaveService');

describe('HR Leave Tracker Frontend Tests', () => {
  test('React_BuildUIComponents_Should render project title', () => {
    render(
      <MemoryRouter>
        <h1>HR Leave Tracker</h1>
      </MemoryRouter>
    );
    expect(screen.getByText(/HR Leave Tracker/i)).toBeInTheDocument();
  });

  test('React_BuildUIComponents_AddLeave component renders form fields', () => {
    render(
      <MemoryRouter>
        <AddLeave />
      </MemoryRouter>
    );

    expect(screen.getByPlaceholderText(/Employee Name/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Reason/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText(/Status/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Add Leave/i })).toBeInTheDocument();
  });

  test('React_BuildUIComponents_ViewLeave component renders leave records', async () => {
    const mockLeaves = [
      {
        leaveId: 1,
        employeeName: 'Alice',
        startDate: '2025-08-01',
        endDate: '2025-08-03',
        reason: 'Vacation',
        status: 'Pending',
      },
    ];
    leaveService.getAllLeaves.mockResolvedValueOnce({ data: mockLeaves });

    render(
      <MemoryRouter>
        <ViewLeave />
      </MemoryRouter>
    );

    expect(await screen.findByText(/Leave Records/i)).toBeInTheDocument();
    expect(await screen.findByText(/Alice/i)).toBeInTheDocument();
    expect(await screen.findByText(/Vacation/i)).toBeInTheDocument();
  });

  test('React_BuildUIComponents_UpdateLeave component renders via route with data', async () => {
    const mockLeaves = [
      {
        id: '1',
        employeeId: '101',
        employeeName: 'Bob',
        startDate: '2025-08-05',
        endDate: '2025-08-08',
        status: 'Approved',
      },
    ];
    leaveService.getAllLeaves.mockResolvedValueOnce({ data: mockLeaves });

    render(
      <MemoryRouter initialEntries={['/update/1']}>
        <Routes>
          <Route path="/update/:id" element={<UpdateLeave />} />
        </Routes>
      </MemoryRouter>
    );

    // ✅ Target only the <h2> tag
    expect(await screen.findByRole('heading', { name: /update leave/i })).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Bob')).toBeInTheDocument();
    expect(await screen.findByDisplayValue('Approved')).toBeInTheDocument();
  });
   // 5. Check if Add Leave button is rendered in AddLeave component
test('React_BuildUIComponents_Check if Add Leave button is rendered in AddLeave component', () => {
  render(
    <MemoryRouter initialEntries={['/add']}>
      <Routes>
        <Route path="/add" element={<AddLeave />} />
      </Routes>
    </MemoryRouter>
  );

  const addButton = screen.getByRole('button', { name: /Add Leave/i });
  expect(addButton).toBeInTheDocument();
});

test('React_APIIntegration_TestingAndAPIDocumentation_Submitting AddLeave form calls leaveService.addLeave', async () => {
  const mockAddLeave = jest.spyOn(leaveService, 'addLeave').mockResolvedValueOnce({ data: {} });

  render(
    <MemoryRouter>
      <AddLeave />
    </MemoryRouter>
  );

  fireEvent.change(screen.getByPlaceholderText(/Employee Name/i), { target: { value: 'Charlie' } });
  fireEvent.change(screen.getByPlaceholderText(/Reason/i), { target: { value: 'Medical' } });
  fireEvent.change(screen.getByPlaceholderText(/Status/i), { target: { value: 'Pending' } });
  fireEvent.click(screen.getByRole('button', { name: /Add Leave/i }));

  await waitFor(() => {
    expect(mockAddLeave).toHaveBeenCalled();
  });

  mockAddLeave.mockRestore();
});
 
test('React_BuildUIComponents_ViewLeave shows empty table when no leave records exist', async () => {
  // Mock the getAllLeaves service to return an empty list
  jest.spyOn(leaveService, 'getAllLeaves').mockResolvedValue({ data: [] });

  render(
    <MemoryRouter>
      <ViewLeave />
    </MemoryRouter>
  );

  // Wait until table is rendered and verify there are no data rows
  await waitFor(() => {
    const rows = screen.getAllByRole('row');
    // Only the header row should be present
    expect(rows.length).toBe(1);
  });
});


});
