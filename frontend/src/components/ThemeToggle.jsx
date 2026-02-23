import { useTheme } from '../context/ThemeContext'

const THEME_ICONS = { light: '\u2600\uFE0F', dark: '\uD83C\uDF19', reading: '\uD83D\uDCD6' }
const THEME_LABELS = { light: 'Light', dark: 'Dark', reading: 'Reading' }

export default function ThemeToggle() {
  const { theme, setTheme, THEMES } = useTheme()

  return (
    <div className="theme-toggle">
      {THEMES.map(t => (
        <button
          key={t}
          className={`theme-btn ${theme === t ? 'active' : ''}`}
          onClick={() => setTheme(t)}
          title={THEME_LABELS[t]}
        >
          {THEME_ICONS[t]}
        </button>
      ))}
    </div>
  )
}
