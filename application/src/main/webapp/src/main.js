import './style.css'
import { boot, renderSsr } from 'scalajs:main.js'

if (typeof window !== 'undefined') {
  window.renderSsr = renderSsr
  boot()
}

export { boot, renderSsr }
