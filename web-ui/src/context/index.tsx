import { createContext } from 'react';

interface ColorPrimaryContextType {
  colorPrimary: string
  setColorPrimary: (color: string) => void
}

export const ColorPrimaryContext = createContext<ColorPrimaryContextType | undefined>(undefined)