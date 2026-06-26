import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post('/api/chat', async ({ request }) => {
    const body = (await request.json()) as { content?: string };
    return HttpResponse.json({
      conversationId: '550e8400-e29b-41d4-a716-446655440000',
      userMessage: {
        id: '1',
        role: 'USER',
        content: body.content ?? '',
        createdAt: new Date().toISOString(),
      },
      assistantMessage: {
        id: '2',
        role: 'ASSISTANT',
        content: `Recebi sua mensagem: ${body.content}`,
        createdAt: new Date().toISOString(),
      },
    });
  }),

  http.get('/api/conversations/:id', ({ params }) => {
    return HttpResponse.json({
      id: params.id,
      title: 'Test',
      messages: [],
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }),

  http.post('/api/documents/upload', () => {
    return HttpResponse.json(
      {
        id: '1',
        originalFilename: 'test.pdf',
        contentType: 'application/pdf',
        sizeBytes: 1024,
        uploadedAt: new Date().toISOString(),
      },
      { status: 201 },
    );
  }),

  http.get('/api/documents', () => {
    return HttpResponse.json({ documents: [], total: 0 });
  }),
];
