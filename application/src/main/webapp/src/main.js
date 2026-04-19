import './style.css'
import { boot, renderSsr } from 'scalajs:main.js'

if (typeof window !== 'undefined') {
  boot()
}

export { boot, renderSsr }
