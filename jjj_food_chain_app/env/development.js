let url = 'http://127.0.0.1:8891';
if (process.env.NODE_ENV != 'development') {
	url = '/api';
}
export default {
	url
}