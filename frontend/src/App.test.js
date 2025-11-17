import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

test('앱이 로그인 화면의 기본 텍스트를 렌더링한다', () => {
  render(<App />);

  // "로그인" 제목(heading)이 렌더링되는지 확인
  const heading = screen.getByRole('heading', { name: /로그인/i });
  expect(heading).toBeInTheDocument();
});

