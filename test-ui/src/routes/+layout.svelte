<script>
	import { page } from '$app/stores';
	import './styles.css';
</script>

<div class="app">
	<nav class="sidebar">
		<h1>PocketAccount</h1>
		<ul>
			<li class:active={$page.url.pathname === '/upload'}>
				<a href="/upload">Last opp</a>
			</li>
			<li class:active={$page.url.pathname === '/interpret'}>
				<a href="/interpret">Tolk</a>
			</li>
			<li class:active={$page.url.pathname === '/results'}>
				<a href="/results">Resultater</a>
			</li>
		</ul>
	</nav>
	<main class="content">
		<slot />
	</main>
</div>

<style>
	.app {
		display: flex;
		height: 100vh;
	}

	.sidebar {
		width: 250px;
		background: #2c3e50;
		color: white;
		padding: 20px;
		box-shadow: 2px 0 5px rgba(0, 0, 0, 0.1);
	}

	.sidebar h1 {
		font-size: 24px;
		margin: 0 0 30px 0;
		padding-bottom: 20px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.2);
	}

	.sidebar ul {
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.sidebar li {
		margin-bottom: 10px;
	}

	.sidebar a {
		display: block;
		padding: 12px 16px;
		color: white;
		text-decoration: none;
		border-radius: 4px;
		transition: background 0.2s;
	}

	.sidebar a:hover {
		background: rgba(255, 255, 255, 0.1);
	}

	.sidebar li.active a {
		background: #3498db;
	}

	.content {
		flex: 1;
		padding: 30px;
		overflow-y: auto;
		background: #ecf0f1;
	}
</style>
