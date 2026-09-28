import React from 'react';
import { createRoot } from 'react-dom/client';
import '../styles.css';
import iconUrl from '../icon.png';

const features = [
  { icon: '◈', title: 'Новый взгляд', description: 'Цвета, освещение и детали делают знакомый мир Minecraft ещё выразительнее.' },
  { icon: '▦', title: 'Каждый блок важен', description: 'От далёких пейзажей до мелочей под ногами — красота в каждой детали.' },
  { icon: '✳', title: 'Твоя атмосфера', description: 'Исследуй мир, строй и наслаждайся каждой минутой приключения.' },
];

const scenes = [
  { color: 'violet', label: 'MOONLIT NIGHT', title: 'Лунная ночь', description: 'Открой мир после захода солнца.' },
  { color: 'blue', label: 'DEEP BLUE', title: 'Подводный мир', description: 'Исследуй глубины океана.' },
  { color: 'pink', label: 'AFTERGLOW', title: 'Закат', description: 'Остановись и полюбуйся горизонтом.' },
];

function Brand({ small = false }) {
  return <a className="brand" href="#home" aria-label="Moonlight — на главную"><img src={iconUrl} alt="" width={small ? 30 : 36} height={small ? 30 : 36} /><span>moonlight<span className="brand-dot">.</span></span></a>;
}

function App() {
  return <>
    <div className="ambient ambient-one" aria-hidden="true" />
    <div className="ambient ambient-two" aria-hidden="true" />
    <header className="site-header">
      <Brand />
      <nav className="nav" aria-label="Основная навигация"><a href="#about">О проекте</a><a href="#features">Возможности</a><a href="#showcase">Галерея</a></nav>
      <a className="header-link" href="#start">Начать <span aria-hidden="true">↗</span></a>
    </header>

    <main>
      <section className="hero wrap" id="home">
        <div className="hero-copy">
          <div className="eyebrow"><span className="live-dot" /> ТВОЙ МИР. ТВОИ ПРАВИЛА.</div>
          <h1>Увидь Minecraft<br /><em>по-новому.</em></h1>
          <p>Визуалы для Minecraft, которые преображают привычный мир. Исследуй новые горизонты, наслаждайся атмосферой и вдохновляйся каждым приключением.</p>
          <div className="hero-actions"><a className="button primary" href="#showcase">Смотреть визуалы <span aria-hidden="true">↗</span></a><a className="button subtle" href="#about"><span className="play-icon" aria-hidden="true">▶</span> Узнать больше</a></div>
          <div className="hero-note"><span className="tiny-stars" aria-hidden="true">✦ ✧ ✦</span> Сделано для тех, кто любит Minecraft</div>
        </div>
        <div className="hero-art" aria-label="Абстрактный светящийся лунный визуал" role="img">
          <div className="art-grid" /><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="moon" /><div className="moon-shine" />
          <div className="art-star star-a">✦</div><div className="art-star star-b">✧</div><div className="art-star star-c">✦</div>
          <div className="art-caption"><span className="caption-icon">✦</span><span><strong>Moonlight visuals</strong><small>Новый взгляд на Minecraft</small></span><span className="bars" aria-hidden="true">{Array.from({ length: 5 }, (_, i) => <i key={i} />)}</span></div>
        </div>
        <div className="scroll-hint">ЛИСТАЙ ВНИЗ <span aria-hidden="true">↓</span></div>
      </section>

      <div className="ticker" aria-hidden="true"><div>EXPLORE YOUR WORLD <span>✦</span> BUILD YOUR STORY <span>✦</span> SEE MINECRAFT DIFFERENTLY <span>✦</span> EXPLORE YOUR WORLD <span>✦</span> BUILD YOUR STORY <span>✦</span></div></div>

      <section className="section wrap about" id="about"><div className="section-kicker">01 / О ПРОЕКТЕ</div><div className="about-layout"><h2>Знакомый мир.<br /><span>Новые впечатления.</span></h2><div><p>Moonlight — визуалы для Minecraft, созданные для тех, кто хочет по-новому взглянуть на любимую игру. Больше атмосферы, ярких моментов и вдохновения для твоих приключений.</p><a className="text-link" href="#features">Открой возможности <span aria-hidden="true">↗</span></a></div></div></section>

      <section className="section wrap" id="features"><div className="section-kicker">02 / ВОЗМОЖНОСТИ</div><div className="section-heading"><h2>Больше, чем<br /><span>просто картинка.</span></h2><p>Погрузись в атмосферу Minecraft и взгляни на любимую игру иначе.</p></div><div className="feature-grid">{features.map((feature, i) => <article className="feature-card" key={feature.title}><span className="feature-icon">{feature.icon}</span><span className="feature-number">{String(i + 1).padStart(2, '0')}</span><h3>{feature.title}</h3><p>{feature.description}</p></article>)}</div></section>

      <section className="section wrap showcase" id="showcase"><div className="section-kicker">03 / ГАЛЕРЕЯ</div><div className="section-heading"><h2>Открой свой<br /><span>мир заново.</span></h2><p>Три настроения для твоих приключений в Minecraft.</p></div><div className="showcase-grid">{scenes.map((scene, i) => <article className={`scene scene-${scene.color}`} key={scene.color}><div className="scene-shape" /><div className="scene-content"><span>{String(i + 1).padStart(2, '0')} / {scene.label}</span><h3>{scene.title}</h3><p>{scene.description}</p></div></article>)}</div></section>

      <section className="cta wrap" id="start"><div className="cta-glow" aria-hidden="true" /><span className="section-kicker">ТВОЁ ПРИКЛЮЧЕНИЕ НАЧИНАЕТСЯ</span><h2>Заходи в мир.<br /><em>Взгляни иначе.</em></h2><p>Открой новые грани Minecraft вместе с Moonlight.</p><a className="button primary" href="#showcase">Исследовать визуалы <span aria-hidden="true">↗</span></a></section>
    </main>
    <footer className="footer wrap"><Brand small /><span>Создано с любовью к Minecraft и его мирам.</span><a href="#home">Наверх ↑</a></footer>
  </>;
}

createRoot(document.getElementById('root')).render(<App />);
