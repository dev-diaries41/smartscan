# SmartScan - Co zbývá dodělat

## ✅ HOTOVO - UI Mockup implementace (2025-01-31)

1. ✅ **Unified Search Bar** - Query Type toggle inline v search baru
2. ✅ **Active Filters Chips** - horizontální scrollable chips pod search barem
3. ✅ **Filter FAB** - FloatingActionButton s Bottom Sheet pro filtry
4. ✅ **Media Type NavigationBar** - TabRow dole (Images/Videos)
5. ✅ **Správná ikona FAB** - FilterList místo ArrowDropDown

---

## 🔧 ZBÝVÁ DODĚLAT - UI/UX vylepšení

### 1. **Threshold Slider Optimalizace** (5 minut, STŘEDNÍ priorita)
- **Problém**: Threshold slider zabírá místo i když není potřeba
- **Řešení**:
  - Přesunout do Advanced Filters v Bottom Sheet
  - NEBO zobrazovat jen když je aktivní vyhledávání
  - NEBO přesunout do Settings jako globální nastavení
- **Dopad**: Více prostoru pro výsledky

### 2. **Loading States UX** (15 minut, STŘEDNÍ priorita)
- **Současný stav**: Prázdné obrazovky během načítání
- **Řešení**:
  - Skeleton screens pro search results
  - Shimmer efekty během indexování
  - Loading indicators na správných místech
- **Dopad**: Lepší UX, méně "prázdných" stavů

### 3. **NSFW Filter Toggle UI** (10 minut, NÍZKÁ priorita)
- Přidat do Active Filters Chips když je aktivní
- Vizuální indikace že NSFW obsah je filtrován

### 4. **Clear Results Action** (5 minut, NÍZKÁ priorita)
- Byl odstraněn z top baru
- Možnosti:
  - Přidat do overflow menu (⋮)
  - Swipe-to-dismiss gesture
  - Nebo nechat úplně pryč (výsledky se mažou automaticky při novém search)

---

## 📋 TECHNICKÝ DLUH

### Code Quality
- [ ] Odstranit deprecated `Divider` → `HorizontalDivider` (TagEditScreen.kt)
- [ ] Odstranit deprecated `ClipboardManager` → `Clipboard` (DonateScreen.kt)
- [ ] Fix deprecated `Icons.Filled.Label` → `Icons.AutoMirrored.Filled.Label`

### Testy
- [ ] Unit testy pro nové komponenty (UnifiedSearchBar, ActiveFiltersChips)
- [ ] UI testy pro Bottom Sheet s filtry
- [ ] Integration testy pro TabRow navigation

---

## 🚀 BUDOUCÍ FEATURES (nízká priorita)

1. **Advanced Search Suggestions**
   - History based suggestions
   - Auto-complete pro tagy

2. **Batch Operations**
   - Multi-select improvements
   - Bulk tagging/moving

3. **Performance Optimizations**
   - LazyColumn optimizations
   - Image loading optimizations

---

## 📝 POZNÁMKY

- Všechny UI změny jsou podle `ui-ux-analysis.html` mockupu
- Active Filters Chips používají `secondaryContainer` color scheme
- FAB se zobrazuje pouze v IMAGE mode
- TabRow je implementován přes Scaffold.bottomBar (Material 3 pattern)
- String resources jsou plně lokalizované (EN/CS)

---

**Poslední update**: 2025-01-31
**Status**: UI mockup HOTOV, zbývají optimalizace a polish
