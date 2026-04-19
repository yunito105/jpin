import { useState } from 'react'
import { ProductList } from './components/ProductList'
import { ProductDetail } from './components/ProductDetail'
import './App.css'

const API_MODE = import.meta.env.VITE_API_MODE ?? 'backend'
const MODE_LABEL: Record<string, string> = {
  wiremock: 'Approach 1: WireMock (Docker)',
  backend: 'Approach 2: Fixed Controller (Spring Boot)',
}

function App() {
  const [selectedProductId, setSelectedProductId] = useState<string | null>(null)

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-inner">
          <h1>商品管理システム</h1>
          <span className="mode-badge">{MODE_LABEL[API_MODE] ?? API_MODE}</span>
        </div>
      </header>

      <main className="app-main">
        {selectedProductId ? (
          <ProductDetail
            productId={selectedProductId}
            onBack={() => setSelectedProductId(null)}
          />
        ) : (
          <ProductList onSelect={setSelectedProductId} />
        )}
      </main>
    </div>
  )
}

export default App

