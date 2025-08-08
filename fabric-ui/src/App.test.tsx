import React from 'react';
import { render } from '@testing-library/react';
import { screen } from '@testing-library/dom';
import '@testing-library/jest-dom';
import App from './App';

test('renders batch configuration tool', () => {
  render(<App />);
  const titleElement = screen.getByText(/Batch Configuration Tool/i);
  expect(titleElement).toBeInTheDocument();
});