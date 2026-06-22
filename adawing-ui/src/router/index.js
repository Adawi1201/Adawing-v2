import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/VisitorLayout.vue'),
      children: [
        { path: '', name: 'Home', component: () => import('@/views/visitor/HomeView.vue') },
        { path: 'articles/:id', name: 'Article', component: () => import('@/views/visitor/ArticleView.vue') },
        { path: 'chronicle', name: 'Chronicle', component: () => import('@/views/visitor/ChronicleView.vue') },
        { path: 'notes', name: 'Notes', component: () => import('@/views/visitor/NotesView.vue') },
        { path: 'messages', name: 'Messages', component: () => import('@/views/visitor/MessagesView.vue') },
        { path: 'about', name: 'About', component: () => import('@/views/visitor/AboutView.vue') }
      ]
    },
    {
      path: '/yusal/admin/login',
      name: 'AdminLogin',
      component: () => import('@/views/admin/LoginView.vue')
    },
    {
      path: '/yusal/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'Dashboard', component: () => import('@/views/admin/DashboardView.vue') },
        { path: 'articles', name: 'AdminArticles', component: () => import('@/views/admin/ArticlesView.vue') },
        { path: 'articles/new', name: 'AdminArticleNew', component: () => import('@/views/admin/ArticleEditorView.vue') },
        { path: 'articles/:id', name: 'AdminArticleEdit', component: () => import('@/views/admin/ArticleEditorView.vue') },
        { path: 'review', name: 'AdminReview', component: () => import('@/views/admin/ReviewView.vue') },
        { path: 'messages', name: 'AdminMessages', component: () => import('@/views/admin/MessagesView.vue') },
        { path: 'tags', name: 'AdminTags', component: () => import('@/views/admin/TagsView.vue') },
        { path: 'moments', name: 'AdminMoments', component: () => import('@/views/admin/MomentsView.vue') },
        { path: 'resources', name: 'AdminResources', component: () => import('@/views/admin/ResourcesView.vue') },
        { path: 'settings', name: 'AdminSettings', component: () => import('@/views/admin/SettingsView.vue') },
        { path: 'account', name: 'AdminAccount', component: () => import('@/views/admin/AccountView.vue') }
      ]
    }
  ]
})

export default router
